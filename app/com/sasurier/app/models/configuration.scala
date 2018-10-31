package com.sasurier.app.models

import javax.inject.Inject
import play.api.Configuration
import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

class CommonConfiguration @Inject()(config: Configuration) {

  type StringSeqOpt = Option[Seq[String]]
  type StringOpt = Option[String]
  type IntOpt = Option[Int]
  type LongOpt = Option[Long]
  type BoolOpt = Option[Boolean]

  def getString(keyName: String): StringOpt = config.get[StringOpt](keyName)

  def getRequiredString(keyName: String): String = getRequired(keyName, s => config.get[StringOpt](s))

  def getRequiredStringSeq(keyName: String): Seq[String] = getRequired(keyName, config.get[StringSeqOpt])

  def getStringSeq(keyName: String): Seq[String] = config.get[StringSeqOpt](keyName).getOrElse(Nil)

  def getBoolean(keyName: String): Option[Boolean] = config.get[BoolOpt](keyName)

  def getRequiredBoolean(keyName: String): Boolean = getRequired(keyName, config.get[BoolOpt])

  def getInt(keyName: String): Option[Int] = config.get[IntOpt](keyName)

  def getRequiredInt(keyName: String): Int = getRequired(keyName, config.get[IntOpt])

  def getLong(keyName: String): Option[Long] = config.get[LongOpt](keyName)

  def getRequiredLong(keyName: String): Long = getRequired(keyName, config.get[LongOpt)


  private def getRequired[T](keyName: String, configFun: String => Option[T]): T = configFun(keyName)
    .getOrElse(throw new ConfigurationNotFoundException(s"Configuration value not found: $keyName"))


  def getScheduleConfig(
    configSegment: String,
    enabledDefault: Boolean = false,
    intervalDefault: FiniteDuration = 60.seconds,
    batchSizeDefault: Int = 200,
    delayDefault: FiniteDuration = 60.seconds) = {
    val schedulerPrefix = "coemtra.schedulers"
    val enabled = getBoolean(s"$schedulerPrefix.$configSegment.enabled").getOrElse(enabledDefault)
    val interval = getInt(s"$schedulerPrefix.$configSegment.intervalSeconds").map(_.seconds).getOrElse(intervalDefault)
    val batchSize = getInt(s"$schedulerPrefix.$configSegment.batchSize").getOrElse(batchSizeDefault)
    val delay = getInt(s"$schedulerPrefix.$configSegment.delaySeconds").map(_.seconds).getOrElse(delayDefault)
    ScheduleConfig(enabled, interval, batchSize, delay)
  }

}

case class ScheduleConfig(enabled: Boolean, interval: FiniteDuration, batchSize: Int, delay: FiniteDuration)

class ConfigurationNotFoundException(message: String)
  extends InternalServerException(ServerErrorCode.ServerConfigurationError, message)