package coursier.cli.publish

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.{PosixFilePermission, PosixFilePermissions}

import coursier.util.Task

import scala.collection.JavaConverters._

final case class GpgSigner(command: String = "gpg", extraOptions: Seq[String] = Nil) extends Signer {
  def sign(content: Content): Task[Either[String, String]] =
    Task.delay {

      val path = content.pathOpt.getOrElse {
        ???
      }

      // inspired by https://github.com/jodersky/sbt-gpg/blob/853e608120eac830068bbb121b486b7cf06fc4b9/src/main/scala/Gpg.scala

      val dest = Files.createTempFile(
        "signer",
        ".asc",
        PosixFilePermissions.asFileAttribute(
          Set(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
          ).asJava
        )
      )

      try {
        val pb = new ProcessBuilder()
          .command(
            Seq(command) ++
              extraOptions ++
              Seq("--armor", "--yes", "--output", dest.toAbsolutePath.toString, "--detach-sign", path.toAbsolutePath.toString): _*
          )
          .inheritIO()

        val p = pb.start()

        val retCode = p.waitFor()

        if (retCode == 0)
          Right(new String(Files.readAllBytes(dest), StandardCharsets.UTF_8))
        else
          Left(s"gpg failed (return code: $retCode)")
      } finally {
        Files.delete(dest)
      }
    }
}
