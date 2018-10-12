package coursier.cli.publish

import caseapp._
import cats.data.Validated
import coursier.cli.publish.options.PublishOptions
import coursier.cli.publish.params.PublishParams

object Publish extends CaseApp[PublishOptions] {
  def run(options: PublishOptions, args: RemainingArgs): Unit =
    PublishParams(options) match {

      case Validated.Invalid(errors) =>
        for (err <- errors.toList)
          Console.err.println(err)
        sys.exit(1)

      case Validated.Valid(params) =>

        val fileSet = FileSet(
          Seq(

          )
        )

        ???
    }
}
