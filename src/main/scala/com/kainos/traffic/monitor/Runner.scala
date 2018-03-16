package com.kainos.traffic.monitor

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

import scala.concurrent.Future

object Runner extends App with KafkaProducer with ConfigurationLoader with Downloader with RequestSources with Extractor with Parameterizer {

  implicit val actorSystem = ActorSystem("monitor")
  implicit val materializer = ActorMaterializer()

  import actorSystem.dispatcher

  val endpoints = loadEndpoints.filterNot(_.ignore)

  val statusActor = actorSystem.actorOf(Props[Status])

  val downloader: Endpoint => Future[String] = downloadEndpoint(_, statusActor)

  val httpRoutes = new HttpRoutes(endpoints, statusActor)

  val bindingFuture = Http().bindAndHandle(httpRoutes.routes, "localhost", 8080)

  val kafka = Sink.ignore // kafkaProducer

  val stream =
    createEndpointDownloadEventSource(endpoints, new Ops(downloader, extract, parameterize))
    .mapAsync(5) { endpoint =>
      downloader(endpoint).map((endpoint, _))
    }
    .map {
      case (endpoint, content) => createRecord(endpoint, content)
    }
    .runWith(kafka)
    .onComplete {
      case result => {
        println(result)
        bindingFuture
          .flatMap(_.unbind())
          .onComplete(_ => {
            actorSystem.terminate()
          })
      }
    }

}