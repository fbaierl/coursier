package coursier.cli.publish.params

import cats.data.{Validated, ValidatedNel}
import cats.implicits._
import coursier.cli.publish.options.RepositoryOptions
import coursier.core.Authentication
import coursier.maven.MavenRepository
import coursier.util.Parse

final case class RepositoryParams(
  repository: MavenRepository,
  sonatypeRestActions: Boolean
)

object RepositoryParams {

  val sonatypeBase = "https://oss.sonatype.org/"
  val sonatypeReleasesStaging = s"${sonatypeBase}service/local/staging/deploy/maven2"

  def apply(options: RepositoryOptions): ValidatedNel[String, RepositoryParams] = {

    val credentialsV = (options.user, options.password) match {
      case (None, None) =>
        Validated.validNel(None)
      case (Some(user), Some(password)) =>
        Validated.validNel(Some(Authentication(user, password)))
      case (Some(user), None) =>
        Validated.invalidNel(s"User $user specified, but no password provided")
      case (None, Some(_)) =>
        Validated.invalidNel("Password provided, but no user specified")
    }

    val repositoryV =
      options.repository.orElse(
        if (options.sonatype.contains(true))
          Some(sonatypeReleasesStaging)
        else
          None
      ) match {
        case None =>
          Validated.invalidNel("No repository specified, and --sonatype option not specified")
        case Some(repoUrl) =>
          Parse.repository(repoUrl) match {
            case Left(err) =>
              Validated.invalidNel(err)
            case Right(m: MavenRepository) =>
              Validated.validNel(m)
            case Right(_) =>
              Validated.invalidNel(s"$repoUrl: non-maven repositories not supported")
          }
      }

    val sonatypeRestActions =
      options.sonatype.getOrElse {
        repositoryV.toOption.exists(_.root.startsWith(sonatypeBase))
      }

    (repositoryV, credentialsV).mapN {
      (repository, credentials) =>
        RepositoryParams(
          repository.copy(
            authentication = credentials
          ),
          sonatypeRestActions
        )
    }
  }
}
