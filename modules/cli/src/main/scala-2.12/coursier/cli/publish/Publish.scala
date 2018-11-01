package coursier.cli.publish

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.Instant

import caseapp._
import cats.data.Validated
import coursier.cli.publish.options.PublishOptions
import coursier.cli.publish.params.PublishParams
import coursier.core.{ModuleName, Organization}
import coursier.util.{Schedulable, Task}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object Publish extends CaseApp[PublishOptions] {

  def pomModuleVersion(params: PublishParams, now: Instant): Either[String, (Content, Organization, ModuleName, String)] =
    (params.metadata.organization, params.metadata.name, params.metadata.version) match {
      case (Some(org), Some(name), Some(ver)) =>

        val content = params.singlePackage.pomOpt match {
          case Some(path) =>
            Content.File(path)
          case None =>
            val pomStr = Pom.create(
              org, name, ver, dependencies = params.metadata.dependencies.getOrElse(Nil).map {
                case (org0, name0, ver0) =>
                  (org0, name0, ver0, None)
              }
            )
            Content.InMemory(now, pomStr.getBytes(StandardCharsets.UTF_8))
        }

        Right((content, org, name, ver))

      case (orgOpt, nameOpt, verOpt) =>
        params.singlePackage.pomOpt match {
          case None =>
            Left(s"Either specify organization / name / version, or pass a POM file.")
          case Some(path) =>
            val s = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)

            val elem = scala.xml.XML.loadString(s) // can throw…
            val xml = coursier.core.compatibility.xmlFromElem(elem)

            val pomOrError =
              for {
                _ <- (if (xml.label == "project") Right(()) else Left("Project definition not found")).right
                proj <- coursier.maven.Pom.project(xml).right
              } yield proj

            pomOrError match {
              case Left(err) => Left(s"Error parsing $path: $err")
              case Right(proj) =>
                val org = orgOpt.getOrElse(proj.module.organization)
                val name = nameOpt.getOrElse(proj.module.name)
                val ver = verOpt.getOrElse(proj.version)
                val content =
                  if (params.metadata.isEmpty)
                    Content.File(path)
                  else {

                    var elem0 = elem
                    elem0 = orgOpt.fold(elem0)(Pom.overrideOrganization(_, elem0))
                    elem0 = nameOpt.fold(elem0)(Pom.overrideModuleName(_, elem0))
                    elem0 = verOpt.fold(elem0)(Pom.overrideVersion(_, elem0))

                    val pomStr = Pom.print(elem0)
                    Content.InMemory(now, pomStr.getBytes(StandardCharsets.UTF_8))
                  }

                Right((content, org, name, ver))
            }
        }
    }

  def run(options: PublishOptions, args: RemainingArgs): Unit =
    PublishParams(options) match {

      case Validated.Invalid(errors) =>
        for (err <- errors.toList)
          Console.err.println(err)
        sys.exit(1)

      case Validated.Valid(params) =>

        val now = Instant.now()

        pomModuleVersion(params, now) match {
          case Left(err) =>
            Console.err.println(err)
            sys.exit(1)
          case Right((pom, org, name, ver)) =>

            val dir = FileSet.Path(org.value.split('.').toSeq ++ Seq(name.value, ver))

            val jarOpt = params
              .singlePackage
              .jarOpt
              .map { path =>
                (dir / s"${name.value}-$ver.jar", Content.File(path))
              }

            val artifacts = params
              .singlePackage
              .artifacts
              .map {
                case (classifier, ext, path) =>
                  val suffix =
                    if (classifier.isEmpty) ""
                    else "-" + classifier.value
                  (dir / s"${name.value}-$ver$suffix.${ext.value}", Content.File(path))
              }

            val fileSet0 = FileSet(
              Seq((dir / s"${name.value}-$ver.pom", pom)) ++
                jarOpt.toSeq ++
                artifacts
            )

            val (upload, repo) = {
              val root = params.repository.repository.root
              if (root.startsWith("/") || root.startsWith("./"))
                (FileUpload(Paths.get(root).toAbsolutePath), params.repository.repository.copy(root = "."))
              else if (root.startsWith("file:"))
                (FileUpload(Paths.get(new URI(root)).toAbsolutePath), params.repository.repository.copy(root = "."))
              else {
                val pool = Schedulable.fixedThreadPool(4) // sizing…
                (OkhttpUpload.create(pool), params.repository.repository)
              }
            }

            val logger = Upload.Logger(Console.err)

            val signerOpt: Option[Signer] =
              if (params.signature.gpg)
                Some(GpgSigner())
              else
                None

            val task = for {
              withChecksums <- {
                if (params.checksum.checksums.isEmpty)
                  Task.point(fileSet0)
                else
                  Checksums(params.checksum.checksums, fileSet0, now)
                    .map(fileSet0 ++ _)
              }
              finalFileSet <- signerOpt.fold(Task.point(withChecksums)){ signer =>
                signer
                  .signatures(withChecksums, now)
                  .flatMap {
                    case Left((path, _, msg)) => Task.fail(new Exception(
                      s"Failed to sign $path: $msg"
                    ))
                    case Right(fs) => Task.point(withChecksums ++ fs)
                  }
              }
              res <- upload.uploadFileSet(repo, finalFileSet, logger)
            } yield res

            val f = task.future()(ExecutionContext.global)
            Await.result(f, Duration.Inf)
        }
    }
}
