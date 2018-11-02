package coursier.cli.publish.sbt

import java.nio.file.Paths

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object SbtApp {
  def main(args: Array[String]): Unit = {
    val s = new Sbt(
      Paths.get(args(0)).toFile,
      Paths.get(args(1)).toFile,
      ExecutionContext.global
    )

    val start = System.currentTimeMillis()
    Await.result(s.publishTo(Paths.get(args(2)).toFile, silent = false), Duration.Inf)
    val end = System.currentTimeMillis()
    Console.err.println(s"Duration: ${end - start} ms")
  }
}
