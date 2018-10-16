package coursier.cli.publish

import java.nio.charset.StandardCharsets
import java.time.Instant

import coursier.util.Task

object Checksums {

  def apply(types: Seq[ChecksumType], fileSet: FileSet, now: Instant): Task[FileSet] = {

    // separate base files from existing checksums
    val filesOrChecksums = fileSet
      .elements
      .map {
        case (path, content) =>
          types
            .collectFirst {
              case t if path.elements.lastOption.exists(_.endsWith("." + t.extension)) =>
                (path.mapLast(_.stripSuffix("." + t.extension)), t)
            }
            .toLeft((path, content))
      }

    val checksums = filesOrChecksums
      .collect {
        case Left(e) => e
      }
      .toSet

    val files = filesOrChecksums
      .collect {
        case Right(p) => p
      }

    // compute missing checksum files
    val checksumFilesTask = Task.gather.gather {
      for {
        type0 <- types
        (path, content) <- files
        if !checksums((path, type0))
      } yield
        content.contentTask.map { b =>
          val checksumPath = path.mapLast(_ + "." + type0.extension)
          val checksum = Checksum.compute(type0, b)
          (checksumPath, Content.InMemory(now, checksum.repr.getBytes(StandardCharsets.UTF_8)))
        }
    }

    checksumFilesTask.map { elements =>
      FileSet(elements)
    }
  }

}
