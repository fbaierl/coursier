package coursier
package cli

import caseapp.core.app.CommandAppA
import coursier.cli.publish.Publish
import coursier.cli.publish.sonatype.{CreateStagingRepository, ListProfiles}
import coursier.cli.resolve.Resolve
import shapeless._

object Coursier extends CommandAppA(CoursierCommand.parser, CoursierCommand.help) {

  override val appName = "Coursier"
  override val progName = "coursier"
  override val appVersion = coursier.util.Properties.version

  def runA =
    args => {
      case Inl(bootstrapOptions) =>
        Bootstrap.run(bootstrapOptions, args)
      case Inr(Inl(fetchOptions)) =>
        Fetch.run(fetchOptions, args)
      case Inr(Inr(Inl(launchOptions))) =>
        Launch.run(launchOptions, args)
      case Inr(Inr(Inr(Inl(publishOptions)))) =>
        Publish.run(publishOptions, args)
      case Inr(Inr(Inr(Inr(Inl(resolveOptions))))) =>
        Resolve.run(resolveOptions, args)
      case Inr(Inr(Inr(Inr(Inr(Inl(sonatypeListProfilesOptions)))))) =>
        ListProfiles.run(sonatypeListProfilesOptions, args)
      case Inr(Inr(Inr(Inr(Inr(Inr(Inl(createStagingRepositoryOptions))))))) =>
        CreateStagingRepository.run(createStagingRepositoryOptions, args)
      case Inr(Inr(Inr(Inr(Inr(Inr(Inr(Inl(sparkSubmitOptions)))))))) =>
        SparkSubmit.run(sparkSubmitOptions, args)
      case Inr(Inr(Inr(Inr(Inr(Inr(Inr(Inr(cnil)))))))) =>
        cnil.impossible
    }

}
