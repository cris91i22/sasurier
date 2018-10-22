package com.sasurier.app.models

import com.sasurier.app.common.{Id, UID}
import com.sasurier.app.models.ApiUserRole.ApiUserRole
import com.sasurier.app.models.ApiUserStatus.ApiUserStatus

case class ApiUser(
  id: Id[ApiUser],
  uid: UID[ApiUser],
  apiToken: String,
  status: ApiUserStatus,
  roles: Seq[ApiUserRole])

object ApiUserRole extends Enumeration {
  type ApiUserRole = Value

  val Customer = Value(1, "Customer")
  val Admin = Value(2, "Admin")
}

object ApiUserStatus extends Enumeration {

  type ApiUserStatus = Value

  val Inactive = Value(0, "Inactive")
  val Active = Value(1, "Active")
}

case class ApiUserRef()