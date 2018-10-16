package coursier.cli.publish

import java.math.BigInteger

final case class Checksum(`type`: ChecksumType, value: BigInteger)

object Checksum {

  def compute(`type`: ChecksumType, content: Array[Byte]): Checksum =
    Checksum(`type`, ???)

}
