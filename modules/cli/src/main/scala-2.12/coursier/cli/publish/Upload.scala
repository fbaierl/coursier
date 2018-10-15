package coursier.cli.publish

import coursier.util.Task

trait Upload {
  // TODO Support chunked content
  def upload(url: String, content: Array[Byte]): Task[Option[Upload.Error]]

  final def uploadFileSet(baseUrl: String, fileSet: FileSet): Task[Unit] = {

    val baseUrl0 = baseUrl.stripSuffix("/")

    // TODO Add exponential back off for transient errors

    // uploading stuff sequentially for now
    // stops at first error
    val uploads = fileSet
      .elements
      .foldLeft(Task.point(Option.empty[(FileSet.Path, Content, Upload.Error)])) {
        case (acc, (path, content)) =>
          val url = s"$baseUrl/${path.elements.mkString("/")}"

          for {
            previousErrorOpt <- acc
            errorOpt <- {
              previousErrorOpt
                .map(e => Task.point(Some(e)))
                .getOrElse(content.contentTask.flatMap(b => upload(url, b).map(_.map((path, content, _)))))
            }
          } yield errorOpt
      }

    ???
  }
}

object Upload {

  sealed abstract class Error(val transient: Boolean) extends Product with Serializable

  object Error {
    final case class InternalServerError(code: Int) extends Error(transient = true)
  }

}
