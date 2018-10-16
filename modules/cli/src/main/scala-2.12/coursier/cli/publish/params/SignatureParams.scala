package coursier.cli.publish.params

import cats.data.{Validated, ValidatedNel}
import coursier.cli.publish.options.SignatureOptions

final case class SignatureParams(
  gpg: Boolean
)

object SignatureParams {
  def apply(options: SignatureOptions): ValidatedNel[String, SignatureParams] = {
    Validated.validNel(
      SignatureParams(
        options.gpg
      )
    )
  }
}
