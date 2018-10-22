package com.sasurier.app.oauth

import com.sasurier.app.common.UID
import com.sasurier.app.models.ApiUser
import java.util.Base64
import play.api.mvc.Request
import scala.util.{Success, Try}

class BasicAuthUtil {

    private val authorizationHeaderName = "Authorization"
    private val authorizationRegex = """Basic (.+)""".r
    private val uidApiTokenRegex = """(.+):(.+)""".r

    def extractCredentials(request: Request[_]): Option[Credentials] = {
      request.headers.get(authorizationHeaderName) match {
        case Some(authorizationRegex(base64)) => extractCredentials(base64)
        case _ => None
      }
    }

    private def extractCredentials(base64: String) = {
      val decodedCredentials = Try(new String(Base64.getDecoder.decode(base64)))
      decodedCredentials match {
        case Success(uidApiTokenRegex(uid, apiToken)) =>
          Some(Credentials(UID(uid), apiToken))
        case _ =>
          None
      }
    }

}


case class Credentials(userUID: UID[ApiUser], apiToken: String)