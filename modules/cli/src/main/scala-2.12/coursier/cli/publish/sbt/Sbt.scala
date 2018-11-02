package coursier.cli.publish.sbt

import java.io.File
import java.nio.file.Files

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.XML

final class Sbt(directory: File, plugin: File, ec: ExecutionContext) {

  def run(sbtCommands: String, silent: Boolean = true) = {

    val processCommands = Seq("sbt", sbtCommands) // UTF-8…

    Try {
      val b = new ProcessBuilder(processCommands.asJava)
      b.directory(directory)
      if (!silent) {
        b.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        b.redirectError(ProcessBuilder.Redirect.INHERIT)
      }
      val p = b.start()
      p.waitFor()
    }
  }


  def structure(silent: Boolean = true) = {

    implicit val ec0 = ec

    // blocking (shortly, but still)
    val dest = Files.createTempFile("coursier-publish-sbt-structure", ".xml")

    val optString = "prettyPrint"

    val setCommands = Seq(
      s"""shellPrompt := { _ => "" }""",
      s"""SettingKey[_root_.scala.Option[_root_.sbt.File]]("sbtStructureOutputFile") in _root_.sbt.Global := _root_.scala.Some(_root_.sbt.file("$dest"))""",
      s"""SettingKey[_root_.java.lang.String]("sbtStructureOptions") in _root_.sbt.Global := "$optString""""
    ).mkString("set _root_.scala.collection.Seq(", ",", ")")

    val sbtCommands = Seq(
      setCommands,
      s"""apply -cp "${plugin.getAbsolutePath}" org.jetbrains.sbt.CreateTasks""",
      s"*/*:dumpStructure"
    ).mkString(";", ";", "")

    Future {
      run(sbtCommands, silent = silent) match {
        case Success(0) =>
          Future {
            try new String(java.nio.file.Files.readAllBytes(dest), "UTF-8")
            finally {
              Files.deleteIfExists(dest)
            }
          }
        case Success(n) =>
          Future.failed(new Exception(s"sbt exited with code $n"))
        case Failure(e) =>
          Future.failed(e)
      }
    }.flatten
  }

  def projects(silent: Boolean = true) = {

    implicit val ec0 = ec

    structure(silent = silent).map { s =>
      val x = XML.loadString(s)
      x.child.collect {
        case n if n.label == "project" =>
          n.child.find(_.label == "id").map(_.text)
      }.flatten
    }
  }

  def publishTo(dir: File, projectsOpt: Option[Seq[String]] = None, silent: Boolean = true) = {

    implicit val ec0 = ec

    for {
      projs <- projectsOpt.map(Future.successful).getOrElse {
        projects(silent = silent)
      }
      cmd = {

        val setCommands = projs.map { p =>
          s"""_root_.sbt.Keys.publishTo in _root_.sbt.LocalProject("$p") := """ +
            s"""_root_.scala.Some("tmp" at "${dir.getAbsoluteFile.toURI.toASCIIString}")"""
        }.mkString("set _root_.scala.collection.Seq(", ",", ")")

        Seq(setCommands, "publish").mkString(";", ";", "")
      }
      _ <- run(cmd, silent = silent) match {
        case Success(0) =>
          Future.successful(())
        case Success(n) =>
          Future.failed(new Exception(s"sbt exited with code $n"))
        case Failure(e) =>
          Future.failed(e)
      }
    } yield ()
  }

}
