package com.kainos.traffic.monitor

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.kainos.traffic.monitor.RequestSourceCreator.Ops

import scala.concurrent.{ExecutionContext, Future}

object Runner {

  def main(args: Array[String]): Unit = {
    new Runner()
  }

}

class Runner(
              configLoader: ConfigurationLoader = new ConfigurationLoader,
              downloader: Downloader = new Downloader,
              requestSourceCreator: RequestSourceCreator = new RequestSourceCreator,
              extractor: Extractor = new Extractor,
              parameterizer: Parameterizer = new Parameterizer,
              kafkaProducer: KafkaProducer = new KafkaProducer,
              routesCreator: (List[Endpoint], ActorRef, ExecutionContext) => HttpRoutes = new HttpRoutes(_, _)(_)) {

  implicit val actorSystem = ActorSystem("monitor")
  implicit val materializer = ActorMaterializer()

  import actorSystem.dispatcher

  val endpoints = configLoader.loadEndpoints.filterNot(_.ignore)

  val statusActor = actorSystem.actorOf(Props[Status])

  val endpointDownloader: Endpoint => Future[String] = downloader.downloadEndpoint(_, statusActor)

  val httpRoutes = routesCreator(endpoints, statusActor, actorSystem.dispatcher)

  val bindingFuture = httpRoutes.bind()

  val kafka = Sink.ignore // kafkaProducer.kafkaProducer // Sink.ignore

  val downloadEventSource = requestSourceCreator.createEndpointDownloadEventSource(endpoints, new Ops(endpointDownloader, extractor.extract, parameterizer.parameterize))

  val stream =
    downloadEventSource
      .mapAsync(30) { endpoint =>
        endpointDownloader(endpoint).map((endpoint, _))
      }
      .map { case (endpoint, content) =>
        kafkaProducer.createRecord(endpoint, content)
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