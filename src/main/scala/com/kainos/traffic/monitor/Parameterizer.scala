package com.kainos.traffic.monitor

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

trait Parameterizer {

  def parameterize(endpoint: Endpoint, extractions: Map[String, List[String]]): List[Endpoint] = {
    endpoint.params.foldLeft(List(endpoint)) {
      case (currentEndpoints, param) =>

        val values = param.paramType match {
          case "extracted" => extractions(param.name)
          case "enumerated" => param.values
          case "date" => dateParams(param)
        }

        currentEndpoints.flatMap { currentEndpoint =>
          values.map { v =>
            val newUrl = currentEndpoint.url.replaceAll(s"\\{${param.name}\\}", v)
            currentEndpoint.copy(url = newUrl)
          }
        }
    }
  }

  private def dateParams(param: Param) = {
    param.values.map {
      case "yesterday" => LocalDateTime.now().minusDays(1)
      case "today" => LocalDateTime.now()
    }.map(_.format(DateTimeFormatter.ofPattern(param.format.getOrElse("YYYY-MM-dd"))))
  }

}
