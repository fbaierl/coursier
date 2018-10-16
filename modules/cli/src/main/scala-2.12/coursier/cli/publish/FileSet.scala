package coursier.cli.publish

final case class FileSet(elements: Seq[(FileSet.Path, Content)]) {
  def ++(other: FileSet): FileSet =
    FileSet(elements ++ other.elements)
  def filterOutExtension(extension: String): FileSet = {
    val suffix = "." + extension
    FileSet(elements.filter(_._1.elements.lastOption.forall(!_.endsWith(suffix))))
  }
}

object FileSet {

  final case class Path(elements: Seq[String]) {
    def /(elem: String): Path =
      Path(elements :+ elem)
    def mapLast(f: String => String): Path =
      Path(elements.dropRight(1) ++ elements.lastOption.map(f).toSeq)
  }

}
