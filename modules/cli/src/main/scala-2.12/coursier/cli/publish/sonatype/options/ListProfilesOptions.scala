package coursier.cli.publish.sonatype.options

import caseapp.Recurse

final case class ListProfilesOptions(
  @Recurse
    sonatype: SonatypeOptions = SonatypeOptions()
)
