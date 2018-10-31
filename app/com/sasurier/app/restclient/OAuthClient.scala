package com.sasurier.app.restclient

import atmos.dsl._
import atmos.dsl.Slf4jSupport._
import com.sasurier.app.models.{CommonConfiguration, InternalServerException, ServerErrorCode}
import com.sasurier.app.utils.CommonClock
import java.time.Instant
import java.util.Base64
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsPath, JsValue, Reads}
import play.api.libs.ws.WSResponse
import play.api.mvc.Results
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class OAuthAuthenticationException(message: String)
  extends InternalServerException(ServerErrorCode.OAuthAuthenticationError, message)

case class OAuthToken(value: String, expiration: Instant, refreshToken: Option[String]) {

  def isValid(clock: CommonClock) = clock.now().isBefore(expiration)
}

case class AuthenticationResponse(
  accessToken: String,
  tokenType: String,
  refreshToken: Option[String],
  expiresIn: Long)

object OAuthClient {
  import play.api.libs.functional.syntax._

  implicit val oauthAuthenticationReads: Reads[AuthenticationResponse] = (
    (JsPath \ "access_token").read[String] and
      (JsPath \ "token_type").read[String] and
      (JsPath \ "refresh_token").readNullable[String] and
      (JsPath \ "expires_in").read[Long]
    )(AuthenticationResponse.apply _)
}

/**
  * Common logic for an OAuth based client.
  * Maintains state so it should only be used with singleton services
  */
trait OAuthClient extends WsClient {
  protected val configPrefix: String
  protected val config: CommonConfiguration
  protected val clock: CommonClock
  protected val logger: Logger

  protected val authUrl: String

  private lazy val host = config.getRequiredString(s"$configPrefix.host")
  private lazy val port = config.getRequiredString(s"$configPrefix.port")
  private lazy val clientId = config.getRequiredString(s"$configPrefix.client_id")
  private lazy val clientSecret = config.getRequiredString(s"$configPrefix.client_secret")
  private lazy val user = config.getString(s"$configPrefix.user")
  private lazy val password = config.getString(s"$configPrefix.password")
  private lazy val retryAttempts = config.getRequiredInt(s"$configPrefix.retry_attempts")
  private lazy val retryDelay = config.getRequiredLong(s"$configPrefix.retry_delay_millis")
  private lazy val timeout = config.getRequiredLong(s"$configPrefix.timeout_millis").millisecond

  private lazy val baseUrl = if(host.nonEmpty && port.nonEmpty) s"$host:$port" else ""
  private lazy val authHeader = Base64.getEncoder.encodeToString(s"$clientId:$clientSecret".getBytes)
  private val contentTypeHeader = "Content-Type" -> "application/json"

  private var maybeToken: Option[OAuthToken] = None

  override protected def defaultRequestTimeout = timeout

  /**
    * Initiate a GET request with the OAuth authentication.
    * The request will automatically use the configured retries.
    */
  protected def oauthGet[T](
    path: String,
    query: Map[String, String] = Map.empty,
    header: Map[String, String] = Map.empty,
    customResponseHandler: WSResponse => Future[WSResponse] = defaultResponseHandler)(
    implicit reads: Reads[T]): Future[T] = retryAsync {
    addHeaders(header).flatMap(apiGet[T](baseUrl + path, query, _, customResponseHandler))
  }

  /**
    * Initiate a POST request with the OAuth authentication.
    * Any retries need to be handled in the caller
    * (can wrap the call with <code>retryAsync</code> to apply the configured policy)
    */
  protected def oauthPost[T](
    path: String,
    body: JsValue,
    query: Map[String, String] = Map.empty,
    header: Map[String, String] = Map.empty,
    customResponseHandler: WSResponse => Future[WSResponse] = defaultResponseHandler)(
    implicit reads: Reads[T]): Future[T] = {
    addHeaders(header).flatMap(apiPost[T](baseUrl + path, body, query, _, customResponseHandler))
  }

  private def addHeaders(header: Map[String, String]) = {
    ensureValidToken().map { token =>
      header + contentTypeHeader + ("Authorization" -> s"Bearer $token")
    }
  }

  private def ensureValidToken() = {
    maybeToken match {
      case Some(token) if token.isValid(clock) => Future.successful(token.value)
      case Some(token) if token.refreshToken.isDefined => refreshOAuthToken(token.refreshToken.get)
      case _ => requestOAuthToken()
    }
  }

  private def refreshOAuthToken(refreshToken: String) = {
    val request = ws.url(baseUrl + authUrl)
      .withHttpHeaders(contentTypeHeader, "Authorization" -> s"Basic $authHeader")
      .withQueryStringParameters(
        "grant_type" -> "refresh_token",
        "refresh_token" -> refreshToken)

    request.post(Results.EmptyContent()).flatMap { response =>
      response.status match {
        case Status.OK => handleTokenResponse(response)
        case Status.UNAUTHORIZED => requestOAuthToken()
        case _ => Future.failed(new OAuthAuthenticationException("Could not refresh the OAuth token"))
      }
    }
  }

  private def requestOAuthToken() = {
    (user, password) match {
      case (Some(u), Some(p)) => requestOAuthTokenWithCredentials(u, p)
      case _ => requestOAuthTokenWithoutCredentials()
    }
  }

  private def requestOAuthTokenWithCredentials(user: String, password: String) = {
    val request = ws.url(baseUrl + authUrl)
      .withHttpHeaders(contentTypeHeader, "Authorization" -> s"Basic $authHeader")
      .withQueryStringParameters(
        "grant_type" -> "password",
        "username" -> user,
        "password" -> password)
      .withRequestTimeout(timeout)

    val requestResult = request.post(Results.EmptyContent())
    requestResult.flatMap { response =>
      response.status match {
        case Status.OK => handleTokenResponse(response)
        case _ => Future.failed(new OAuthAuthenticationException(
          "Could not authenticate to get an OAuth token with password"))
      }
    }
  }

  private def requestOAuthTokenWithoutCredentials() = {
    val request = ws.url(baseUrl + authUrl)
      .withHttpHeaders(contentTypeHeader, "Authorization" -> s"Basic $authHeader")
      .withQueryStringParameters("grant_type" -> "client_credentials")
      .withRequestTimeout(timeout)

    val requestResult = request.post(Results.EmptyContent())
    requestResult.flatMap { response =>
      response.status match {
        case Status.OK => handleTokenResponse(response)
        case _ => Future.failed(new OAuthAuthenticationException(
          "Could not authenticate to get an OAuth token with client credentials"))
      }
    }
  }

  private def handleTokenResponse(response: WSResponse) = {
    import OAuthClient.oauthAuthenticationReads
    parseJson[AuthenticationResponse](response).map { authentication =>
      val expiration = clock.now().plusMillis(authentication.expiresIn).minusSeconds(1)
      val token = authentication.accessToken
      val refreshToken = authentication.refreshToken

      // save the token in the var for subsequent calls
      maybeToken = Option(OAuthToken(token, expiration, refreshToken))

      token
    }
  }

  private lazy val generalRetryPolicy = {
    retryFor {
      retryAttempts.attempts
    } using {
      exponentialBackoff {
        retryDelay.milliseconds
      } randomized {
        100.millis
      }
    } onError {
      case _: Exception => keepRetrying
    } monitorWith logger.logger
  }

  protected def retryAsync[T](code: => Future[T]): Future[T] = {
    generalRetryPolicy.retryAsync() { code }
  }
}
