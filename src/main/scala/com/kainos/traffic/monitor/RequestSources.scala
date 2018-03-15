package com.kainos.traffic.monitor

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util

import akka.actor.ActorRef

import scala.concurrent.duration._
import akka.stream.scaladsl.{Merge, Source}
import com.jayway.jsonpath.JsonPath

trait RequestSources extends Downloader with Utils {

  def createEndpointDownloadEventSource(endpoints: List[Endpoint], statusActor: ActorRef): Source[Endpoint, _] = {
    combine(
      endpoints
      .filter(!_.inner)
      .map(createEndpointDownloadEventSource(_, endpoints, statusActor)))
  }

  private def createEndpointDownloadEventSource(endpoint: Endpoint, endpoints: List[Endpoint], statusActor: ActorRef) = {
    endpoint.trigger.map { trigger =>
      val innerEndpoint = endpoints.filter(_.name == trigger).head
      createComplexSource(endpoint, innerEndpoint, statusActor)
    }.getOrElse(createSimpleTickSource(endpoint))
  }

  private def createSimpleTickSource(endpoint: Endpoint): Source[Endpoint, _] = {
    Source.tick(1 second, endpoint.interval.get seconds, endpoint)
  }

  private def createComplexSource(endpoint: Endpoint, innerEndpoint: Endpoint, statusActor: ActorRef): Source[Endpoint, _] = {
    createSimpleTickSource(endpoint)
      .mapAsync(1)(downloadEndpoint(_, statusActor))
      .flatMapConcat { case (_, content) =>
        endpointsToTrigger(endpoint, content, innerEndpoint)
      }
  }

  private def endpointsToTrigger(endpoint: Endpoint, content: String, endpointToTrigger: Endpoint): Source[Endpoint, _] = {
    val extractions = findAllExtractions(endpoint, content)

    val endpointsToRun = allEndpointsToRun(endpointToTrigger, extractions)

    combine(endpointsToRun.map(createSimpleTickSource))
  }

  private def findAllExtractions(endpoint: Endpoint, content: String) = {
    endpoint.extract.map { extract =>
      (extract.name, extractValues(extract.path, content))
    }.toMap
  }

  private def allEndpointsToRun(endpointToTrigger: Endpoint, extractions: Map[String, List[String]]) = {
    endpointToTrigger.params.foldLeft(List(endpointToTrigger)) {
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

  private def extractValues(extract: String, content: String): List[String] = {
    val json = content
    val javaList: util.List[Object] = JsonPath.parse(json).read(extract)
    val scalaList = scala.collection.JavaConverters.asScalaBuffer(javaList).toList.distinct
    scalaList.map(_.toString)
  }
}
