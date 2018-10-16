package coursier.cli.publish

import java.util.concurrent.ExecutorService

import com.squareup.okhttp.{MediaType, OkHttpClient, Request, RequestBody}
import coursier.Cache
import coursier.core.Authentication
import coursier.util.Task

import scala.util.Try

final case class OkhttpUpload(client: OkHttpClient, pool: ExecutorService) extends Upload {

  import OkhttpUpload.mediaType

  def upload(url: String, authentication: Option[Authentication], content: Array[Byte], logger: Upload.Logger): Task[Option[Upload.Error]] = {

    val body = RequestBody.create(mediaType, content)

    // TODO Add auth
    val request = {
      val b = new Request.Builder()
        .url(url)
        .put(body)

      // Handling this ourselves rather than via client.setAuthenticator / com.squareup.okhttp.Authenticator
      for (auth <- authentication)
        b.addHeader("Authorization", "Basic " + Cache.basicAuthenticationEncode(auth.user, auth.password))

      b.build()
    }

    Task.schedule(pool) {
      logger.uploading(url)

      val res = Try {
        val response = client.newCall(request).execute()

        if (response.isSuccessful)
          None
        else {
          val code = response.code()
          Some(Upload.Error.HttpError(code))
        }
      }

      logger.uploaded(url, res.toEither.fold(e => Some(Upload.Error.DownloadError(e)), x => x))

      res.get
    }
  }
}

object OkhttpUpload {
  private val mediaType = MediaType.parse("application/octet-stream")

  def create(pool: ExecutorService): Upload = {
    // Seems we can't even create / shutdown the client thread pool (via its Dispatcher)…
    OkhttpUpload(new OkHttpClient, pool)
  }
}
