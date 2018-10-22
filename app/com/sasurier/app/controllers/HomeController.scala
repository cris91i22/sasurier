package com.sasurier.app.controllers

import be.objectify.deadbolt.scala.ActionBuilders
import com.sasurier.app.models.ApiUserRole
import com.sasurier.app.services.SecuredApi
import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.Future

@Singleton
class HomeController @Inject()(
  override val actionBuilder: ActionBuilders,
  override val cc: ControllerComponents) extends AbstractController(cc) with SecuredApi {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Ok(com.sasurier.app.views.html.index("Your new application is ready."))
  }

  def index2 = authorized(ApiUserRole.Admin)() { implicit r =>
    Future.successful(Ok(Json.toJson("SEEEEEEEEEEEEEEEEEE" -> "HOLAAAAAAAAA")))
  }

}
