package coursier.cli.publish.params

import cats.data.{Validated, ValidatedNel}
import cats.implicits._
import coursier.cli.publish.options.MetadataOptions
import coursier.core.{ModuleName, Organization}
import coursier.util.Parse

final case class MetadataParams(
  organization: Option[Organization],
  name: Option[ModuleName],
  version: Option[String],
  // TODO Support full-fledged coursier.Dependency?
  dependencies: Seq[(Organization, ModuleName, String)]
)

object MetadataParams {
  def apply(options: MetadataOptions, defaultScalaVersion: String): ValidatedNel[String, MetadataParams] = {

    // TODO Check for invalid character? emptiness?
    val organization = options.organization.map(Organization(_))
    val name = options.name.map(ModuleName(_))
    val version = options.version
    val dependenciesV = options.dependency.traverse { s =>
      Parse.moduleVersion(s, defaultScalaVersion) match {
        case Left(err) =>
          Validated.invalidNel(err)
        case Right((mod, ver)) if mod.attributes.nonEmpty =>
          Validated.invalidNel(s"Dependency $s: attributes not supported for now")
        case Right((mod, ver)) =>
          Validated.validNel((mod.organization, mod.name, ver))
      }
    }

    dependenciesV.map { dependencies =>
      MetadataParams(
        organization,
        name,
        version,
        dependencies
      )
    }
  }
}
