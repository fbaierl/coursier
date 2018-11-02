package coursier.cli.publish.sbt

import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path, Paths}
import java.util.Random
import java.util.concurrent.ConcurrentHashMap

import argonaut._
import argonaut.Argonaut._
import org.scalasbt.ipcsocket.UnixDomainSocket

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, Promise}
import scala.util.{Success, Try}

final class SbtClient(dir: Path) {

  import SbtClient._

  val uri = new String(Files.readAllBytes(Paths.get("/Users/alexandre/projects/coursier/project/target/active.json")), UTF_8)
    .decodeEither(Active.decoder)
    .toOption
    .get // !!
    .uri

  val socket = new UnixDomainSocket(new URI(uri).getSchemeSpecificPart)

  val is = socket.getInputStream
  val os = socket.getOutputStream

  val osLock = new Object

  val messages = new ListBuffer[String]

  val handlers = new ConcurrentHashMap[String, Option[Json] => Unit]

  val readThread: Thread =
    new Thread("sbt-client-read") {
      setDaemon(true)

      val b = new ByteArrayOutputStream

      def tryParse(): Either[String, Option[String]] = {

        val s = new String(b.toByteArray)

        @tailrec
        def parse(idx: Int, lenOpt: Option[Int]): Either[(String, Int), Option[(String, Int)]] = {

          val sepLen = "\r\n".length
          val sepIdx = s.indexOf("\r\n", idx)

          if (sepIdx < idx)
            // no full line
            Right(None)
          else if (sepIdx == idx)
            // empty line, now read content
            lenOpt match {
              case None =>
                Left(("No length found", idx + sepLen))
              case Some(len) =>
                if (idx + sepLen + len <= s.length)
                  Right(Some((s.substring(idx + sepLen, idx + sepLen + len), idx + sepLen + len)))
                else
                  Right(None)
            }
          else {
            // read header
            val h = s.substring(idx, sepIdx)
            if (h.startsWith("Content-Length: "))
              Try(h.drop("Content-Length: ".length).toInt).toEither match {
                case Left(e) =>
                  Left((s"Error reading content length: $e", sepIdx + sepLen))
                case Right(n) =>
                  parse(sepIdx + sepLen, Some(n))
              }
            else
              parse(sepIdx + sepLen, lenOpt)
          }
        }

        parse(0, None) match {
          case Left((err, idx)) =>
            val c = b.toByteArray.drop(idx)
            b.reset()
            b.write(c)
            Left(err)
          case Right(None) =>
            Right(None)
          case Right(Some((m, idx))) =>
            val c = b.toByteArray.drop(idx)
            b.reset()
            b.write(c)
            Right(Some(m))
        }
      }

      @tailrec
      def parseAll(): Unit =
        tryParse() match {
          case Left(err) =>
            Console.err.println(s"Error handling message from sbt server: $err")
            parseAll()
          case Right(None) =>
            ()
          case Right(Some(m)) =>
            // parse, match with handlers
            m.decodeEither(RpcMessage.decoder) match {
              case Left(e) =>
                Console.err.println(s"Error decoding message from sbt server: $e")
              case Right(m) =>
                if (m.id.nonEmpty)
                  Console.err.println(s"Got response $m")
                for {
                  id <- m.id
                  h <- Option(handlers.get(id))
                } {
                  handlers.remove(id)
                  h(m.result)
                }
            }
            parseAll()
        }

      override def run() = {
        val buf = Array.ofDim[Byte](1024)
        var read = 0
        while ({
          read = is.read(buf)
          read >= 0
        }) {
          b.write(buf, 0, read)
          parseAll()
        }
      }
    }

  readThread.start()


  val random = new Random

  def randomId(): String =
    random.nextInt().toString

  def init(): Future[Unit] = {

    val p = Promise[Unit]()

    val id = randomId()

    val m = RpcMessage(
      id = Some(id),
      jsonrpc = "2.0",
      method = Some("initialize"),
      params = Some(Json.obj("initializationOptions" -> Json.obj()))
    )

    val b = RpcMessage.encoder(m).nospaces.getBytes(UTF_8)

    val s =
      s"Content-Length: ${b.length + 2}\r\n\r\n".getBytes("UTF-8") ++
        b ++
        "\r\n".getBytes("UTF-8")

    handlers.put(id, { _ =>
      p.tryComplete(Success(()))
    })

    // !! add timeout
    // !! blocking
    osLock.synchronized {
      os.write(s)
    }

    p.future
  }

  def send(cmd: String): Future[Option[Json]] = {

    val p = Promise[Option[Json]]()

    val id = randomId()

    val m = RpcMessage(
      id = Some(id),
      jsonrpc = "2.0",
      method = Some("sbt/exec"),
      params = Some(Exec.encoder(Exec(cmd)))
    )

    val b = RpcMessage.encoder(m).nospaces.getBytes(UTF_8)

    val s =
      s"Content-Length: ${b.length + 2}\r\n\r\n".getBytes("UTF-8") ++
        b ++
        "\r\n".getBytes("UTF-8")

    handlers.put(id, { s =>
      p.tryComplete(Success(s))
    })

    // !! add timeout
    // !! blocking
    osLock.synchronized {
      os.write(s)
    }

    p.future
  }

}

object SbtClient {

  import argonaut.ArgonautShapeless._

  private final case class RpcMessage(
    jsonrpc: String,
    id: Option[String] = None,
    method: Option[String] = None,
    params: Option[Json] = None,
    result: Option[Json] = None
  )

  private object RpcMessage {
    implicit val decoder = DecodeJson.of[RpcMessage]
    implicit val encoder = EncodeJson.of[RpcMessage]
  }

  private final case class Exec(commandLine: String)

  private object Exec {
    implicit val encoder = EncodeJson.of[Exec]
  }

  private case class Active(uri: String)

  private object Active {
    implicit val decoder = DecodeJson.of[Active]
  }

}
