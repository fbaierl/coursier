package coursier.cli.publish.sonatype

import argonaut._
import argonaut.Argonaut._
import com.squareup.okhttp.{OkHttpClient, Request}
import coursier.Cache
import coursier.core.Authentication
import coursier.util.Task

final case class SonatypeApi(client: OkHttpClient, base: String, authentication: Option[Authentication]) {

  import SonatypeApi._

  private def get[T: DecodeJson](path: String): Task[T] = {

    val url = s"$base/$path"

    val request = {
      val b = new Request.Builder().url(url)

      // Handling this ourselves rather than via client.setAuthenticator / com.squareup.okhttp.Authenticator
      for (auth <- authentication)
        b.addHeader("Authorization", "Basic " + Cache.basicAuthenticationEncode(auth.user, auth.password))

      b.addHeader("Accept", "application/json,application/vnd.siesta-error-v1+json,application/vnd.siesta-validation-errors-v1+json")

      b.build()
    }

    val t = Task.delay {
      Console.err.println(s"Getting $url")
      val resp = client.newCall(request).execute()
      Console.err.println(s"Done: $url")

      if (resp.isSuccessful)
        resp.body().string().decodeEither[T] match {
          case Left(e) =>
            Task.fail(new Exception(s"Received invalid response from $url: $e"))
          case Right(t) =>
            Task.point(t)
        }
      else
        Task.fail(new Exception(s"Failed to get $url (http status: ${resp.code()})"))
    }

    t.flatMap(identity)
  }

  def listProfiles(): Task[Seq[SonatypeApi.Profile]] =
    get("staging/profiles")(Profiles.decode)
      .map(_.data.map(_.profile))

}

object SonatypeApi {

  final case class Profile(
    id: String,
    name: String,
    uri: String
  )

  object Profiles {

    import argonaut.ArgonautShapeless._

    final case class Response(data: List[Profile])

    final case class Profile(
      id: String,
      name: String,
      resourceURI: String
    ) {
      def profile =
        SonatypeApi.Profile(
          id,
          name,
          resourceURI
        )
    }

    implicit val decode = DecodeJson.of[Response]
  }

}
