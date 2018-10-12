package coursier.cli.publish

import caseapp._

final case class PublishOptions(

)

object PublishOptions {
  implicit val parser = Parser[PublishOptions]
  implicit val help = caseapp.core.help.Help[PublishOptions]
}
