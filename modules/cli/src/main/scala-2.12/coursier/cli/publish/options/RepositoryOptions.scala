package coursier.cli.publish.options

import caseapp._

final case class RepositoryOptions(

  @Name("r")
  @Name("repo")
    repository: Option[String] = None,

  user: Option[String] = None,
  password: Option[String] = None,

  sonatype: Option[Boolean] = None

) {
  override def toString: String =
    Seq(
      repository,
      user,
      "****",
      sonatype
    ).mkString("RepositoryOptions(", ", ", ")")
}

object RepositoryOptions {
  implicit val parser = Parser[RepositoryOptions]
  implicit val help = caseapp.core.help.Help[RepositoryOptions]
}
