package com.inneractive.cerberus

import java.io.File

import com.typesafe.config.ConfigFactory

/**
  * Created by Richard Grossman on 2016/07/27.
  */
trait CliStarter {
  def cliAnalyse(args : Array[String]) = {
    val parser = new scopt.OptionParser[CerberusConfig]("cerberusengine") {
      head("cerberus", "1.x")

      opt[String]('c', "config").action((x, c) =>
        c.copy(configFilePath = x)).text("application.conf full path")
      opt[String]('d', "model").required().action((x, c) =>
        c.copy(dateModel = x)).text("Date of the model to load for prediction format YYYYMMDD")
    }

    val appConfig = parser.parse(args, CerberusConfig())

    val tsc = appConfig map { c =>
      if (c.configFilePath.isEmpty) ConfigFactory.load()
      else ConfigFactory.parseFile(new File(c.configFilePath))
    } get

    (appConfig, tsc)
  }
}

case class CerberusConfig(configFilePath: String = "", dateModel: String = "")
