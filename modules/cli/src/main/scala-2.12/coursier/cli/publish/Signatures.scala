package coursier.cli.publish

import coursier.util.Task

object Signatures {

  def apply(fileSet: FileSet): Task[FileSet] =
    // TODO
    Task.point(fileSet)

}
