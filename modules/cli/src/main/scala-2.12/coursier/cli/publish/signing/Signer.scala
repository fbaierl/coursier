package coursier.cli.publish.signing

import java.nio.charset.StandardCharsets
import java.time.Instant

import coursier.cli.publish.{Content, FileSet}
import coursier.util.Task

/**
  * Signs artifacts.
  */
trait Signer {

  /**
    * Computes the signature of the passed `content`.
    *
    * @return: an error message (left), or the signature file content (right), wrapped in [[Task]]
    */
  def sign(content: Content): Task[Either[String, String]]

  /**
    * Adds missing signatures in a [[FileSet]].
    *
    * @param fileSet: [[FileSet]] to add signatures to - can optionally contain some already calculated signatures
    * @param now: last modified time for the added signature files
    * @return a [[FileSet]] of the missing signature files
    */
  def signatures(fileSet: FileSet, now: Instant): Task[Either[(FileSet.Path, Content, String), FileSet]] = {

    val elementsOrSignatures = fileSet.elements.map {
      case (path, content) =>
        if (path.elements.lastOption.exists(_.endsWith(".asc")))
          Right(path.mapLast(_.stripSuffix(".asc")))
        else
          Left((path, content))
    }

    val signed = elementsOrSignatures
      .collect {
        case Right(path) => path
      }
      .toSet

    val toSign = elementsOrSignatures
      .collect {
        case Left((path, content)) if !signed(path) =>
          (path, content)
      }

    val signaturesTask =
      toSign.foldLeft(Task.point[Either[(FileSet.Path, Content, String), List[(FileSet.Path, Content)]]](Right(Nil))) {
        case (acc, (path, content)) =>
          for {
            previous <- acc
            res <- {
              previous match {
                case l @ Left(_) => Task.point(l)
                case Right(l) =>
                  sign(content).map {
                    case Left(e) =>
                      Left((path, content, e))
                    case Right(s) =>
                      Right((path.mapLast(_ + ".asc"), Content.InMemory(now, s.getBytes(StandardCharsets.UTF_8))) :: l)
                  }
              }
            }
          } yield res
      }

    signaturesTask.map(_.right.map { elements =>
      FileSet(elements.reverse)
    })
  }

}
