package coursier.cli.publish

import java.io.PrintStream

import coursier.core.Authentication
import coursier.maven.MavenRepository
import coursier.util.Task

trait Upload {
  // TODO Support chunked content
  def upload(url: String, authentication: Option[Authentication], content: Array[Byte], logger: Upload.Logger): Task[Option[Upload.Error]]

  final def uploadFileSet(repository: MavenRepository, fileSet: FileSet, logger: Upload.Logger): Task[Seq[(FileSet.Path, Content, Upload.Error)]] = {

    val baseUrl0 = repository.root.stripSuffix("/")

    // TODO Add exponential back off for transient errors

    // uploading stuff sequentially for now
    // stops at first error
    fileSet
      .elements
      .foldLeft(Task.point(Option.empty[(FileSet.Path, Content, Upload.Error)])) {
        case (acc, (path, content)) =>
          val url = s"$baseUrl0/${path.elements.mkString("/")}"

          for {
            previousErrorOpt <- acc
            errorOpt <- {
              previousErrorOpt
                .map(e => Task.point(Some(e)))
                .getOrElse(content.contentTask.flatMap(b => upload(url, repository.authentication, b, logger).map(_.map((path, content, _)))))
            }
          } yield errorOpt
      }
      .map(_.toSeq)
  }
}

object Upload {

  sealed abstract class Error(val transient: Boolean) extends Product with Serializable

  object Error {
    final case class HttpError(code: Int) extends Error(transient = code / 100 == 5)
    final case class DownloadError(exception: Throwable) extends Error(transient = false)
  }

  trait Logger {
    def uploading(url: String): Unit
    def uploaded(url: String, errorOpt: Option[Error]): Unit
  }

  object Logger {
    val nop: Logger =
      new Logger {
        def uploading(url: String) = {}
        def uploaded(url: String, errorOpt: Option[Error]) = {}
      }

    def apply(ps: PrintStream): Logger =
      new Logger {
        def uploading(url: String) =
          ps.println(s"Uploading $url")
        def uploaded(url: String, errorOpt: Option[Error]) = {
          val msg = errorOpt match {
            case None =>
              s"Uploaded $url"
            case Some(error) =>
              s"Failed to upload $url: $error"
          }
          ps.println(msg)
        }
      }
  }

}
