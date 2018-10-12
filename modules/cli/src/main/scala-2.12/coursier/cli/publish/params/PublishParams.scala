package coursier.cli.publish.params

import cats.data.ValidatedNel
import cats.implicits._
import coursier.cli.publish.options.PublishOptions

final case class PublishParams(
  repository: RepositoryParams,
  metadata: MetadataParams,
  singlePackage: SinglePackageParams
)

object PublishParams {
  def apply(options: PublishOptions): ValidatedNel[String, PublishParams] = {

    // FIXME Get from options
    val defaultScalaVersion = scala.util.Properties.versionNumberString

    val repositoryV = RepositoryParams(options.repositoryOptions)
    val metadataV = MetadataParams(options.metadataOptions, defaultScalaVersion)
    val singlePackageV = SinglePackageParams(options.singlePackageOptions)

    (repositoryV, metadataV, singlePackageV).mapN {
      (repository, metadata, singlePackage) =>
        PublishParams(
          repository,
          metadata,
          singlePackage
        )
    }
  }
}
