package coursier.cli.publish

final case class FileSet(elements: Seq[(FileSet.Path, Content)])

object FileSet {

  final case class Path(elements: Seq[String])

}
