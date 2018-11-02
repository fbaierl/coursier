package coursier.cli.publish

import java.io.{BufferedWriter, OutputStreamWriter, PrintWriter}
import java.nio.file.{Path, Paths}

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

final class Sbt(directory: Path, plugin: Path, ec: ExecutionContext) {

  val dest = java.nio.file.Files.createTempFile("coursier-publish-sbt-structure", ".xml")

  def run() = {

    val optString = "prettyPrint"

    val setCommands = Seq(
      s"""shellPrompt := { _ => "" }""",
      s"""SettingKey[_root_.scala.Option[_root_.sbt.File]]("sbtStructureOutputFile") in _root_.sbt.Global := _root_.scala.Some(_root_.sbt.file("$dest"))""",
      s"""SettingKey[_root_.java.lang.String]("sbtStructureOptions") in _root_.sbt.Global := "$optString""""
    ).mkString("set _root_.scala.collection.Seq(", ",", ")")

    val sbtCommands = Seq(
      setCommands,
      s"""apply -cp "${plugin.normalize().toAbsolutePath}" org.jetbrains.sbt.CreateTasks""",
      s"*/*:dumpStructure"
    ).mkString(";", ";", "")

    val processCommands = Seq("sbt") // UTF-8…

    Try {
      val processBuilder = new ProcessBuilder(processCommands.asJava)
      processBuilder.directory(directory.toFile)
      processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
      processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
      val process = processBuilder.start()
      val writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream, "UTF-8")))
      writer.println(sbtCommands)
      // exit needs to be in a separate command, otherwise it will never execute when a previous command in the chain errors
      writer.println("exit")
      writer.flush()
      writer.close()
      process.waitFor()
    }
  }


  def structure() = {

    implicit val ec0 = ec

    Future {
      run() match {
        case Success(0) =>
          Future {
            new String(java.nio.file.Files.readAllBytes(dest), "UTF-8")
          }
        case Success(n) =>
          Future.failed(new Exception(s"sbt exited with code $n"))
        case Failure(e) =>
          Future.failed(e)
      }
    }.flatten
  }

}

object SbtApp {
  def main(args: Array[String]): Unit = {
    val s = new Sbt(
      Paths.get(args(0)),
      Paths.get(args(1)),
      ExecutionContext.global
    )

    val start = System.currentTimeMillis()
    val struct = Await.result(s.structure(), Duration.Inf)
    val end = System.currentTimeMillis()
    println(struct)
    Console.err.println(s"Duration: ${end - start} ms")
  }
}
