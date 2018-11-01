package coursier.cli.publish.sonatype.params

import cats.data.ValidatedNel
import coursier.cli.publish.sonatype.options.ListProfilesOptions

final case class ListProfilesParams(
  sonatype: SonatypeParams
)

object ListProfilesParams {
  def apply(options: ListProfilesOptions): ValidatedNel[String, ListProfilesParams] = {

    val sonatypeV = SonatypeParams(options.sonatype)

    sonatypeV.map { sonatype =>
      ListProfilesParams(
        sonatype
      )
    }
  }
}
