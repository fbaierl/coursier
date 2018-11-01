package coursier.cli.publish.sonatype.options

import caseapp.Recurse

final case class CreateStagingRepositoryOptions(
  profile: Option[String],
  profileId: Option[String],
  description: String = "",
  @Recurse
    sonatype: SonatypeOptions = SonatypeOptions()
)
