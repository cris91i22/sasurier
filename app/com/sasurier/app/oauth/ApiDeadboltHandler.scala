package com.sasurier.app.oauth

import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltHandler}
import com.sasurier.app.common.{Id, UID}
import com.sasurier.app.models.{ApiUser, ApiUserRole, ApiUserStatus}
import com.sasurier.app.utils.IdentificationUtils
import javax.inject.Inject
import play.api.mvc.{Request, Results}

import scala.concurrent.{ExecutionContext, Future}

class ApiDeadboltHandler @Inject()(
   basicAuthUtil: BasicAuthUtil)(
   implicit ec: ExecutionContext) extends DeadboltHandler with Results {

  override def beforeAuthCheck[A](request: Request[A]) = Future.successful(None)

  override def getDynamicResourceHandler[A](request: Request[A]) = Future.successful(None)

  override def getSubject[A](request: AuthenticatedRequest[A]) = {
    basicAuthUtil.extractCredentials(request) match {
      case Some(Credentials(uid, apiToken)) => //TODO: Verify user against DB
        val fakeApiUser = ApiUser(
          Id.uninitialized,
          UID(IdentificationUtils.generateUid(7)),
          "APITOKEN",
          ApiUserStatus.Active,
          Seq(ApiUserRole.Admin))
        if (uid.value + apiToken == "123APITOKEN")
          Future.successful(Some(ApiDeadboltSubject(fakeApiUser)))
        else
          Future.successful(None)
      case _ =>
        Future.successful(None)
    }
  }

  override def onAuthFailure[A](request: AuthenticatedRequest[A]) = Future.successful(Unauthorized)

}
