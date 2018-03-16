package com.kainos.traffic.monitor

import com.typesafe.config.{Config, ConfigFactory, ConfigValue}

import scala.collection.JavaConverters.asScalaBuffer

case class Extraction(name: String, path: String)

case class Param(name: String, paramType: String, values: List[String] = Nil, format: Option[String] = None)

case class Endpoint(name: String, url: String, interval: Int, trigger: Option[String] = None, extractions: List[Extraction] = Nil, params: List[Param] = Nil, inner: Boolean = true, ignore: Boolean = false)

trait ConfigurationLoader {

  def config = ConfigFactory.load

  def loadEndpoints(): List[Endpoint] = {
    asScalaBuffer(config.getConfigList("monitor.endpoints")).map { c =>
      parseEndpoint(c)
    }.toList
  }

  private def parseEndpoint(c: Config) = {
    Endpoint(
      c.getString("name"),
      c.getString("url"),
      c.getInt("intervalSeconds"),
      optional(c, "trigger", c.getString),
      list[Config](c, "extract", c.getConfigList).map(parseExtraction),
      list[Config](c, "params", c.getConfigList).map(parseParams),
      optional(c, "inner", c.getBoolean).getOrElse(false),
      optional(c, "ignore", c.getBoolean).getOrElse(false)
    )
  }

  private def parseExtraction(c: Config) = {
    Extraction(
      c.getString("name"),
      c.getString("path"))
  }

  private def parseParams(c: Config) = {
    Param(
      c.getString("name"),
      c.getString("type"),
      list(c, "values", c.getStringList),
      optional(c, "format", c.getString))
  }

  private def optional[T](config: Config, param: String, extractor: (String) => T): Option[T] = {
    if(config.hasPath(param)) {
      Some(extractor(param))
    } else {
      None
    }
  }

  private def list[T](config: Config, param: String, extractor: (String) => java.util.List[_ <: T]): List[T] = {
    if(config.hasPath(param)) {
      asScalaBuffer(extractor(param)).toList
    } else {
      Nil
    }
  }
}
