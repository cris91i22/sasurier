package com.sasurier.app.restclient

import com.sasurier.app.models.{InternalServerException, ServerErrorCode}
import com.sasurier.app.models.exceptions.ServerErrorCode
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Reads}
import play.api.libs.ws.{WSResponse, WSClient => PlayClient}

import scala.concurrent.duration.{Duration, DurationLong}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait WsClient {
  implicit val executionContext: ExecutionContext
  protected val ws: PlayClient

  protected def defaultRequestTimeout: Duration = 20.seconds

  private lazy val logger = Logger(classOf[WsClient])

  def apiGet[T](
    path: String,
    query: Map[String, String] = Map.empty,
    header: Map[String, String] = Map.empty,
    customResponseHandler: WSResponse => Future[WSResponse] = defaultResponseHandler)(
    implicit reads: Reads[T]): Future[T] = {
    apiCallForResponse[T]("GET", path, query, "", header, customResponseHandler)
  }

  def apiPost[T](
    path: String,
    body: JsValue,
    query: Map[String, String] = Map.empty,
    header: Map[String, String] = Map.empty,
    customResponseHandler: WSResponse => Future[WSResponse] = defaultResponseHandler)(
    implicit reads: Reads[T]): Future[T] = {
    apiCallForResponse[T]("POST", path, query, Json.stringify(body), header, customResponseHandler)
  }

  def apiPost[T](
    path: String,
    query: Map[String, String])(implicit reads: Reads[T]): Future[T] = {
    apiPost[T](path, Json.obj(), query)
  }

  def apiDelete(path: String, query: Map[String, String] = Map.empty): Future[WSResponse] = {
    apiCall("DELETE", path, query)
  }

  private def apiCall(
    method: String,
    path: String,
    query: Map[String, String] = Map.empty,
    body: String = "",
    header: Map[String, String] = Map.empty,
    customResponseHandler: WSResponse => Future[WSResponse] = defaultResponseHandler): Future[WSResponse] = {
    val request =
      ws.url(path)
        .withRequestTimeout(defaultRequestTimeout)
        .withQueryString(query.toSeq: _*)
        .withHeaders(header.toSeq: _*)

    val requestWithBody = if (body.isEmpty) request else request.withBody(body)

    logger.debug(s"Sending $method to $path with $query and $body")
    for {
      response <- requestWithBody.execute(method)
      custom <- customResponseHandler(response)
    } yield custom
  }

  private def apiCallForResponse[T](
     method: String,
     path: String,
     query: Map[String, String] = Map.empty,
     body: String = "",
     header: Map[String, String] = Map.empty,
     customResponseHandler: WSResponse => Future[WSResponse] = defaultResponseHandler)(
     implicit reads: Reads[T]): Future[T] = {
    apiCall(method, path, query, body, header, customResponseHandler).flatMap(parseJson[T])
  }

  protected def parseJson[T](response: WSResponse)(implicit reads: Reads[T]) = {
    Try(response.json) match {
      case Success(json) => json.validate[T].asEither match {
        case Left(errors) =>
          val errorsString = errors.map(e => s"${e._1}: ${e._2.map(_.message)}").mkString(", ")
          Future.failed(new InvalidJsonException(s"$errorsString json: ${json.toString()}"))
        case Right(t) =>
          Future.successful(t)
      }
      case Failure(error) => Future.failed(new InvalidJsonException(error.getMessage))
    }
  }

  protected def defaultResponseHandler(response: WSResponse) = Future.successful(response)
}

class InvalidJsonException(message: String) extends InternalServerException(ServerErrorCode.InvalidJson, message)