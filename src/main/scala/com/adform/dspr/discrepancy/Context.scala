package com.adform.dspr.discrepancy

import java.util.concurrent.TimeUnit

import com.aerospike.client.Host
import com.typesafe.config.{Config, ConfigFactory}

class Context(config: Config) {
  def this() = this(EnvironmentContext.loadConfig())

  val application = new ApplicationSettings(config)
  val vertica = new VerticaSettings(config)
  val aerospike = new AerospikeSettings(config)
  val logs = new LogSettings(config)
}

class ApplicationSettings(config: Config) {
  config.checkValid(ConfigFactory.defaultReference(), "application")
  val numberOfPartitions: Int = config.getInt("application.numberOfPartitions")
}

class VerticaSettings(config: Config) {
  config.checkValid(ConfigFactory.defaultReference(), "vertica")

  val url: String = config.getString("vertica.url")
  val username: String = config.getString("vertica.username")
  val password: String = config.getString("vertica.password")
  val fetchSize: Int = config.getInt("vertica.fetchSize")
}

class AerospikeSettings(config: Config) {
  import scala.collection.JavaConverters._

  config.checkValid(ConfigFactory.defaultReference(), "aerospike")
  val hosts = {
    val ips = config.getStringList("aerospike.ip")
    val port = config.getInt("aerospike.port")
    ips.asScala.map(new Host(_, port)).toArray
  }

  val namespace = config.getString("aerospike.namespace")
  val set = config.getString("aerospike.set")
  val readTimeout = config.getDuration("aerospike.readTimeout", TimeUnit.MILLISECONDS).toInt
}


class LogSettings(config: Config) {
  config.checkValid(ConfigFactory.defaultReference(), "logs")

  val minLogLevel: String = config.getString("logs.minLevel")
  val enableConsoleLogging: Boolean = config.getBoolean("logs.enableConsoleLogging")
}
