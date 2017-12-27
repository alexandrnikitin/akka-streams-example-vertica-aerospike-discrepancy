package com.adform.dspr.discrepancy

import com.typesafe.config.{Config, ConfigFactory}

object EnvironmentContext {
  val Environment = "ENV"

  def getCurrentEnvironment: String = System.getenv().get(Environment)

  def loadConfig(): Config = {
    ConfigFactory.load(s"application.$getCurrentEnvironment")
  }

  def loadConfig(env: String): Config = {
    ConfigFactory.load(s"application.$env")
  }
}
