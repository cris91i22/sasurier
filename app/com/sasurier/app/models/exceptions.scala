package com.sasurier.app.models

import com.sasurier.app.models.ExceptionLevel.ExceptionLevel
import play.api.libs.json.{JsNull, JsObject, Json}

object ExceptionLevel extends Enumeration {
  type ExceptionLevel = Value

  val User, Server = Value
}

abstract class AbstractServerException(
  val code: ServerErrorCode.ServerErrorCode,
  val message: String,
  val level: ExceptionLevel,
  val cause: Throwable = null,
  val data: Option[JsObject] = None) extends RuntimeException(message, cause) {

  def codeValue = code.id

  def isUserError = level == ExceptionLevel.User

  def getData = Json.stringify(data.getOrElse(JsNull))
}

class InternalServerException(code: ServerErrorCode.ServerErrorCode, message: String, cause: Throwable = null)
  extends AbstractServerException(code, message, ExceptionLevel.Server, cause)

object ServerErrorCode extends Enumeration {
  type ServerErrorCode = Value

  val Unknown = Value(1)
  val SiteDownForMaintenance = Value(1099)

  //General errors
  val InvalidJson = Value(1100)
  val ServerConfigurationError = Value(1101)

  // OAuth errors
  val OAuthAuthenticationError = Value(1300)

}