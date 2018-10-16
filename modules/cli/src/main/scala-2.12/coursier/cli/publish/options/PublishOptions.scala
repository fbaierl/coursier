package coursier.cli.publish.options

import caseapp._

final case class PublishOptions(

  @Recurse
    repositoryOptions: RepositoryOptions = RepositoryOptions(),

  @Recurse
    metadataOptions: MetadataOptions = MetadataOptions(),

  @Recurse
    singlePackageOptions: SinglePackageOptions = SinglePackageOptions(),

  @Recurse
    checksumOptions: ChecksumOptions = ChecksumOptions()

)

object PublishOptions {
  implicit val parser = Parser[PublishOptions]
  implicit val help = caseapp.core.help.Help[PublishOptions]
}
