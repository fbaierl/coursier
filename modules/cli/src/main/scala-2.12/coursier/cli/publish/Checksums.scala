package coursier.cli.publish

import coursier.util.Task

object Checksums {

  def apply(`type`: ChecksumType, fileSet: FileSet): Task[FileSet] = {

    val extSuffix = "." + `type`.extension

    val (files, checksums) = fileSet
      .elements
      .partition {
        case (path, _) =>
          path
            .elements
            .lastOption
            .forall(!_.endsWith(extSuffix))
      }

    val hasChecksum = checksums.map(_._1.mapLast(_.stripSuffix(extSuffix))).toSet

    val elementsTask = Task.gather.gather {
      files
        .collect {
          case (path, content) if !hasChecksum(path) =>
            for {
              b <- content.contentTask
            } yield {
              val checksumPath = path.mapLast(_ + extSuffix)
              val checksum = Checksum.compute(`type`, b)
              (checksumPath, Content.InMemory(???, ???))
            }
        }
    }

    elementsTask.map { elements =>
      fileSet ++ FileSet(elements)
    }
  }

}
