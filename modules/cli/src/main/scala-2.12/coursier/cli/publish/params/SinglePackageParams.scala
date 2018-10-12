package coursier.cli.publish.params

import java.nio.file.{Files, Path, Paths}

import cats.data.{Validated, ValidatedNel}
import cats.implicits._
import coursier.cli.publish.options.SinglePackageOptions
import coursier.core.Classifier
import coursier.util.ValidationNel

final case class SinglePackageParams(
  jarOpt: Option[Path],
  pomOpt: Option[Path],
  artifacts: Seq[(Classifier, Path)]
)

object SinglePackageParams {

  private def q = "\""

  def apply(options: SinglePackageOptions): ValidatedNel[String, SinglePackageParams] = {

    // FIXME This does some I/O (not reflected in return type)

    def fileV(path: String): ValidatedNel[String, Path] = {
      val p = Paths.get(path)
      if (!Files.exists(p))
        Validated.invalidNel(s"not found: $path")
      else if (!Files.isRegularFile(p))
        Validated.invalidNel(s"not a regular file: $path")
      else
        Validated.validNel(p)
    }

    def fileOptV(pathOpt: Option[String]): ValidatedNel[String, Option[Path]] =
      pathOpt match {
        case None =>
          Validated.validNel(None)
        case Some(path) =>
          fileV(path).map(Some(_))
      }

    val jarOptV = fileOptV(options.jar)
    val pomOptV = fileOptV(options.pom)

    val artifactsV = options.artifact.traverse { s =>
      s.split(":", 2) match {
        case Array(strClassifier, path) =>
          fileV(path).map((Classifier(strClassifier), _))
        case _ =>
          Validated.invalidNel(s"Maformed artifact argument: $s (expected: ${q}classifier:/path/to/artifact$q)")
      }
    }

    (jarOptV, pomOptV, artifactsV).mapN {
      (jarOpt, pomOpt, artifacts) =>
        SinglePackageParams(
          jarOpt,
          pomOpt,
          artifacts
        )
    }
  }
}
