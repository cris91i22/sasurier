package com.sasurier.app.oauth

import be.objectify.deadbolt.scala.HandlerKey
import be.objectify.deadbolt.scala.cache.HandlerCache
import javax.inject.Inject
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

class ApiDeadboltModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(bind[HandlerCache].to[ApiHandlerCache])
  }
}

class ApiHandlerCache @Inject()(apiHandler: ApiDeadboltHandler) extends HandlerCache {

  override def apply() = apiHandler

  override def apply(key: HandlerKey) = apiHandler
}

