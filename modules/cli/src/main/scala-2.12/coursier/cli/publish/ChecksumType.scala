package coursier.cli.publish

import java.util.Locale

sealed abstract class ChecksumType(
  val name: String,
  val extension: String
) extends Product with Serializable

object ChecksumType {
  case object SHA1 extends ChecksumType("sha-1", "sha1")
  case object MD5 extends ChecksumType("md5", "md5")

  val all = Seq(SHA1, MD5)
  val map = all.map(c => c.name -> c).toMap

  def parse(s: String): Either[String, ChecksumType] =
    map
      .get(s.toLowerCase(Locale.ROOT))
      .toRight(s"Unrecognized checksum: $s")
}
