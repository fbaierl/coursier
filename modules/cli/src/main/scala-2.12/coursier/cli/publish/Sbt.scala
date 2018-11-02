package coursier.cli.publish

import java.nio.file.{Path, Paths}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

final class Sbt(directory: Path, plugin: Path, ec: ExecutionContext) {

  val dest = java.nio.file.Files.createTempFile("coursier-publish-sbt-structure", ".xml")

  val init = s"""set _root_.scala.collection.Seq(
    shellPrompt := { _ => "" },
    SettingKey[_root_.scala.Option[_root_.sbt.File]]("sbtStructureOutputFile") in _root_.sbt.Global := _root_.scala.Some(_root_.sbt.file("$dest")),
    SettingKey[_root_.java.lang.String]("sbtStructureOptions") in _root_.sbt.Global := "prettyPrint"
  )""".replace("\n", "")

  val addPlugin = s"""apply -cp "$plugin" org.jetbrains.sbt.CreateTasks"""

  val dumpStructure = "*/*:dumpStructure"

  val cmd = Seq("reload", init, addPlugin, dumpStructure, "session clear-all").map(";" + _).mkString(" ")


  def structure() = {

    implicit val ec0 = ec
    val client = new SbtClient(directory)

    for {
      _ <- client.init()
      _ <- client.send("z" + cmd)
      _ <- client.send("about")
      c <- Future {
        new String(java.nio.file.Files.readAllBytes(dest), "UTF-8")
      }
    } yield c
  }

}

object SbtApp {
  def main(args: Array[String]): Unit = {
    val s = new Sbt(
      Paths.get(args(0)),
      Paths.get(args(1)),
      ExecutionContext.global
    )

    val struct = Await.result(s.structure(), Duration.Inf)
    println(struct)

    while (true)
      Thread.sleep(10000L)
  }
}
