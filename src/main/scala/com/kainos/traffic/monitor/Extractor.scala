package com.kainos.traffic.monitor

import java.util

import com.jayway.jsonpath.{Configuration, JsonPath, Option}

trait Extractor {

  def extract(content: String, extractions: List[Extraction]): Map[String, List[String]] = {
    extractions.map { extract =>
      (extract.name, extractValues(extract.path, content))
    }.toMap
  }

  private def extractValues(path: String, content: String): List[String] = {
    val json = content
    val javaList: util.List[Object] = JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST)).parse(json).read(path)
    val scalaList = scala.collection.JavaConverters.asScalaBuffer(javaList).toList.distinct
    scalaList.map(_.toString)
  }

}
