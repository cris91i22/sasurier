package com.sasurier.app.oauth

import be.objectify.deadbolt.scala.models.Subject
import com.sasurier.app.models.{ApiUser, ApiUserRole}

case class ApiDeadboltSubject(apiUser: ApiUser) extends Subject {

  override def identifier = apiUser.uid.value

  override def permissions = Nil

  override def roles = {
    apiUser.roles.map {
      case ApiUserRole.Customer => Customer
      case ApiUserRole.Admin => Admin
    }.toList
  }
}
