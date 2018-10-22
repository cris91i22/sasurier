package com.sasurier.app.controllers

import javax.inject.{Inject, Singleton}
import java.time.Instant

import be.objectify.deadbolt.scala.ActionBuilders
import com.sasurier.app.services.SecuredApi
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class HealthCheckController @Inject()(
  override val actionBuilder: ActionBuilders,
  override val cc: ControllerComponents)(
  implicit ec: ExecutionContext) extends AbstractController(cc) with SecuredApi {

  // Careful - note that this controller is not Secured
  def healthCheck = Action { implicit request =>
    val timeString = Instant.now
    val message = s"Good day - the time is - $timeString - build ${com.sasurier.app.build.BuildInfo.version}"
    val json = Json.obj("message" -> message)
    Ok(json)
  }

}