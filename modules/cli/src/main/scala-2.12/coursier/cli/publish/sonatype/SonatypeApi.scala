package coursier.cli.publish.sonatype

import java.nio.charset.StandardCharsets

import argonaut._
import argonaut.Argonaut._
import com.squareup.okhttp.{MediaType, OkHttpClient, Request, RequestBody}
import coursier.Cache
import coursier.core.Authentication
import coursier.util.Task

final case class SonatypeApi(client: OkHttpClient, base: String, authentication: Option[Authentication]) {

  // vaguely inspired by https://github.com/lihaoyi/mill/blob/7b4ced648ecd9b79b3a16d67552f0bb69f4dd543/scalalib/src/mill/scalalib/publish/SonatypeHttpApi.scala

  import SonatypeApi._

  private def post[B: EncodeJson, T: DecodeJson](path: String, content: B): Task[T] = {

    val body = RequestBody.create(SonatypeApi.mediaType, EncodeJson.of[B].apply(content).nospaces.getBytes(StandardCharsets.UTF_8))

    ???
  }

  private def get[T: DecodeJson](path: String, post: Option[RequestBody] = None): Task[T] = {

    val url = s"$base/$path"

    val request = {
      val b = new Request.Builder().url(url)
      for (body <- post)
        b.post(body)

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
        resp.body().string().decodeEither(Response.decode[T]) match {
          case Left(e) =>
            Task.fail(new Exception(s"Received invalid response from $url: $e"))
          case Right(t) =>
            Task.point(t.data)
        }
      else
        Task.fail(new Exception(s"Failed to get $url (http status: ${resp.code()})"))
    }

    t.flatMap(identity)
  }

  def listProfiles(): Task[Seq[SonatypeApi.Profile]] =
    // for w/e reasons, Profiles.Profile.decode isn't implicitly picked
    get("staging/profiles")(DecodeJson.ListDecodeJson(Profiles.Profile.decode))
      .map(_.map(_.profile))

  def createStagingRepository(profile: Profile, description: String) = {
    ???
  }

}

object SonatypeApi {

  final case class Profile(
    id: String,
    name: String,
    uri: String
  )

  private val mediaType = MediaType.parse("application/json")

  private final case class Response[T](data: T)

  private object Response {
    import argonaut.ArgonautShapeless._
    implicit def decode[T: DecodeJson] = DecodeJson.of[Response[T]]
  }

  object Profiles {

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

    object Profile {
      import argonaut.ArgonautShapeless._
      implicit val decode = DecodeJson.of[Profile]
    }
  }

}
