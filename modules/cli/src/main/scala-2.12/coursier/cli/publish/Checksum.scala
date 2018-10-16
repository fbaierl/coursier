package coursier.cli.publish

import java.math.BigInteger
import java.security.MessageDigest

final case class Checksum(`type`: ChecksumType, value: BigInteger) {
  assert(value.compareTo(BigInteger.ZERO) >= 0)
  assert(value.compareTo(BigInteger.valueOf(16L).pow(`type`.size)) < 0)
  def repr: String =
    String.format(s"%0${`type`.size}x", value)
}

object Checksum {

  def compute(`type`: ChecksumType, content: Array[Byte]): Checksum = {

    val md = MessageDigest.getInstance(`type`.name)
    md.update(content)
    val digest = md.digest()
    val calculatedSum = new BigInteger(1, digest)

    Checksum(`type`, calculatedSum)
  }

}
