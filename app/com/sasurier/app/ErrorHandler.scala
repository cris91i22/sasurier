package com.sasurier.app

import javax.inject.Singleton
import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.mvc.Results.{BadRequest, InternalServerError}
import scala.concurrent.Future

@Singleton
class ErrorHandler extends HttpErrorHandler {

  private lazy val logger = Logger(classOf[ErrorHandler])

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    val response = Json.obj(
      "code" -> statusCode,
      "message" -> message
    )
    Future.successful(BadRequest(response))
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    logger.error(s"A server error occurred: ${exception.getMessage}", exception)
    val response = Json.obj(
      "code" -> 500,
      "message" -> "Internal server error"
    )
    Future.successful(InternalServerError(response))
  }

}
