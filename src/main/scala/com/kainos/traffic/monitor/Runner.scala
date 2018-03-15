package com.kainos.traffic.monitor

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink }
import scala.io.StdIn

object Runner extends App with KafkaProducer with ConfigurationLoader with Downloader with RequestSources {

  implicit val actorSystem = ActorSystem("monitor")
  implicit val materializer = ActorMaterializer()

  import actorSystem.dispatcher

  val endpoints = loadEndpoints.filterNot(_.ignore)

  val statusActor = actorSystem.actorOf(Props[Status])

  val kafka = Sink.ignore // kafkaProducer

  val stream =
    createEndpointDownloadEventSource(endpoints, statusActor)
    .mapAsync(5)(downloadEndpoint(_, statusActor))
    .map {
      case (endpoint, content) => createRecord(endpoint, content)
    }
    .log("got msg")
    .runWith(kafka)

  val httpRoutes = new HttpRoutes(endpoints, statusActor)

  val bindingFuture = Http().bindAndHandle(httpRoutes.routes, "localhost", 8080)

  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => {
      actorSystem.terminate()
    })

}