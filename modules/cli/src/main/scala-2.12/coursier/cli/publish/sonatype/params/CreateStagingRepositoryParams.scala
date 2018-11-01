package coursier.cli.publish.sonatype.params

import cats.data.{Validated, ValidatedNel}
import coursier.cli.publish.sonatype.options.CreateStagingRepositoryOptions

final case class CreateStagingRepositoryParams(
  profile: Either[String, String], // left: id, right: name
  description: Option[String]
)

object CreateStagingRepositoryParams {
  def apply(options: CreateStagingRepositoryOptions): ValidatedNel[String, CreateStagingRepositoryParams] = {

    val profileV = (options.profileId, options.profile) match {
      case (Some(id), None) =>
        Validated.validNel(Left(id))
      case (None, Some(name)) =>
        Validated.validNel(Right(name))
      case (Some(_), Some(_)) =>
        Validated.invalidNel("Cannot specify both profile id and profile name")
      case (None, None) =>
        Validated.invalidNel("No profile id or profile name specified")
    }

    val description = Some(options.description).filter(_.nonEmpty)

    profileV.map { profile =>
      CreateStagingRepositoryParams(
        profile,
        description
      )
    }
  }
}
