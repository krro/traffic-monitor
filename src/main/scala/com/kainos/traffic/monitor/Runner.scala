package com.kainos.traffic.monitor

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Merge, Sink, Source}
import com.jayway.jsonpath.JsonPath
import com.kainos.traffic.monitor.Status.{DownloadEnd, DownloadStart}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn

object Runner extends App with KafkaProducer with ConfigurationLoader with Downloader {

  implicit val actorSystem = ActorSystem("monitor")
  implicit val materializer = ActorMaterializer()

  import actorSystem.dispatcher

  val statusActor = actorSystem.actorOf(Props[Status])

  val endpoints = loadEndpoints.filterNot(_.ignore)

  val endpointTickSources = endpoints.filter(e => e.interval.isDefined).map { endpoint =>
    createEndpointDownloadEventSource(endpoint)
  }

  def createEndpointDownloadEventSource(endpoint: Endpoint) = {
    endpoint.trigger.map { trigger =>
      createComplexSource(endpoint, trigger)
    }.getOrElse(createSimpleTickSource(endpoint))
  }

  private def createSimpleTickSource(endpoint: Endpoint): Source[Endpoint, _] = {
    Source.tick(1 second, endpoint.interval.get seconds, endpoint)
  }

  def createComplexSource(endpoint: Endpoint, trigger: String): Source[Endpoint, _] = {
    createSimpleTickSource(endpoint)
      .mapAsync(1)(downloadEndpointWithAudit(_, statusActor))
      .flatMapConcat { case (_, content) =>

        val extractions = endpoint.extract.map { extract =>
          (extract.name, findInnerValues(extract.path, content))
        }.toMap

        val endpointToTrigger = endpoints.filter(_.name == trigger).head

        val endpointsToRun = endpointToTrigger.params.foldLeft(List(endpointToTrigger)) {
          case (currentEndpoints, param) =>

            val values = param.paramType match {
              case "extracted" => extractions(param.name)
              case "enumerated" => param.values
              case "date" => {
                param.values.map {
                  case "yesterday" => LocalDateTime.now().minusDays(1)
                  case "today" => LocalDateTime.now()
                }.map(_.format(DateTimeFormatter.ofPattern(param.format.getOrElse("YYYY-MM-dd"))))
              }
            }

            currentEndpoints.flatMap { currentEndpoint =>
              values.map { v =>
                val newUrl = currentEndpoint.url.replaceAll(s"\\{${param.name}\\}", v)
                currentEndpoint.copy(url = newUrl)
              }
            }
        }

        println(endpointsToRun.size)

        Source(endpointsToRun)
      }
  }

  val endpointsTicks = Source.combine(Source.empty[Endpoint], Source.empty[Endpoint], endpointTickSources: _*)(Merge(_))

  def downloadEndpointWithAudit(endpoint: Endpoint, statusActor: ActorRef): Future[(Endpoint, String)] = {
    statusActor ! DownloadStart(endpoint.name)

    val futureResult = downloadEndpoint(endpoint.url).map((endpoint, _))

    futureResult.onComplete {
      case _ => statusActor ! DownloadEnd(endpoint.name)
    }

    futureResult
  }

  def findInnerValues(extract: String, content: String): List[String] = {
    val json = content
    val javaList: util.List[Object] = JsonPath.parse(json).read(extract)
    val scalaList = scala.collection.JavaConverters.asScalaBuffer(javaList).toList.distinct
    scalaList.map(_.toString)
  }

  val contentSource: Source[(Endpoint, String), _] = endpointsTicks.mapAsync(5)(downloadEndpointWithAudit(_, statusActor))

  val messageSource: Source[ProducerMessage, _] = contentSource.map { data =>
    val (endpoint, byteString) = data
    createRecord(endpoint, byteString)
  }

  val sink = Sink.ignore // kafkaProducer

  val streamComplete = messageSource
    .log("got msg")
    .runWith(sink)

  val httpRoutes = new HttpRoutes(endpoints, statusActor)

  val bindingFuture = Http().bindAndHandle(httpRoutes.routes, "localhost", 8080)

  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => {
      actorSystem.terminate()
    })

}