package coursier.cli

import caseapp.CommandParser
import caseapp.core.help.CommandsHelp
import coursier.cli.publish.Publish
import coursier.cli.resolve.Resolve

object CoursierCommand {

  val parser =
    CommandParser.nil
      .add(Bootstrap)
      .add(Fetch)
      .add(Launch)
      .add(Publish)
      .add(Resolve)
      .add(SparkSubmit)
      .reverse

  val help =
    CommandsHelp.nil
      .add(Bootstrap)
      .add(Fetch)
      .add(Launch)
      .add(Publish)
      .add(Resolve)
      .add(SparkSubmit)
      .reverse

}
