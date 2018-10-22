package com.sasurier.app.services

import be.objectify.deadbolt.scala.{ActionBuilders, AuthenticatedRequest}
import com.sasurier.app.common.UID
import com.sasurier.app.models.ApiUserRef
import com.sasurier.app.models.ApiUserRole.ApiUserRole
import com.sasurier.app.oauth.ApiDeadboltSubject
import play.api.mvc._
import scala.concurrent.Future
import scala.language.reflectiveCalls

trait SecuredApi { self: BaseController =>

  def actionBuilder: ActionBuilders
  def cc: ControllerComponents

  /**
    * Utility method to execute an action, provided the caller was authenticated and has the required roles
    * @param roles The required roles to execute the action (joined with AND, i.e. all roles are required)
    * @param block The action to execute
    * @return
    */
  def authorized[A](
    roles: ApiUserRole*)(
    parser: BodyParser[A] = parse.anyContent)(
    block: ApiUserRequest[A] => Future[Result]): Action[A] = {
    actionBuilder
      .RestrictAction(roles.map(_.toString): _*)(cc.parsers)
      .defaultHandler.apply(parser)(handleAuthorizedRequest(block, _))
  }

  private def handleAuthorizedRequest[A](
    block: ApiUserRequest[A] => Future[Result],
    request: AuthenticatedRequest[A]) = request.subject match {
    case Some(user: ApiDeadboltSubject) =>
      block(ApiUserRequest(UID[ApiUserRef](user.identifier), request))

    case other =>
      // It will always be Some(something) because it's a RestrictAction
      // We don't have another implementation of Subject, so it must be an AdminApiSubject
      // This branch is expected to be dead code
      throw new IllegalStateException(s"Expected AuthApiUser, got: $other")
  }
}

case class ApiUserRequest[A](apiUserRef: UID[ApiUserRef], req: Request[A])
  extends WrappedRequest[A](req)