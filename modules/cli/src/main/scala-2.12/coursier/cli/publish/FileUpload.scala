package coursier.cli.publish

import java.nio.file.{Files, Path}

import coursier.core.Authentication
import coursier.util.Task

final case class FileUpload(base: Path) extends Upload {
  private val base0 = base.normalize()
  def upload(
    url: String,
    authentication: Option[Authentication],
    content: Array[Byte],
    logger: Upload.Logger
  ): Task[Option[Upload.Error]] = {

    val p = base0.resolve(url).normalize()
    if (p.startsWith(base0))
      Task.delay {
        Files.createDirectories(p.getParent)
        Files.write(p, content)
        None
      }
    else
      Task.fail(new Exception(s"Invalid path: $url (base: $base0, p: $p)"))
  }
}
