package coursier.cli.publish.options

import caseapp._

final case class SinglePackageOptions(

  @Name("J")
    jar: Option[String] = None,

  @Name("P")
    pom: Option[String] = None,

  @Name("A")
  @ValueDescription("classifier:/path/to/file")
    artifact: List[String] = Nil

)

object SinglePackageOptions {
  implicit val parser = Parser[SinglePackageOptions]
  implicit val help = caseapp.core.help.Help[SinglePackageOptions]
}
