package com.sasurier.app.oauth

import be.objectify.deadbolt.scala.models.Role

sealed trait ApiDeadboltRole extends Role

case object Customer extends ApiDeadboltRole {

  override def name = "Customer"
}

case object Admin extends ApiDeadboltRole {

  override def name = "Admin"
}
