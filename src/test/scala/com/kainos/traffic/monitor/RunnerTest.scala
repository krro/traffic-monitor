package com.kainos.traffic.monitor

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import org.scalatest.WordSpec
import org.scalatest.mockito.MockitoSugar.mock
import org.mockito.Mockito.when
import org.mockito.Mockito.verify

import scala.concurrent.{ExecutionContext, Future}
import org.mockito.ArgumentMatchers._
import org.mockito.ArgumentMatchers.{eq => isEq}

class RunnerTest extends WordSpec {

  trait Test {

    val configLoader = mock[ConfigurationLoader]
    val downloader = mock[Downloader]
    val requestSourceCreator = mock[RequestSourceCreator]
    val extractor = mock[Extractor]
    val parameterizer = mock[Parameterizer]
    val kafkaProducer = mock[KafkaProducer]
    val routesCreator = mock[(List[Endpoint], ActorRef, ExecutionContext) => HttpRoutes]

    def run = new Runner(
      configLoader,
      downloader,
      requestSourceCreator,
      extractor,
      parameterizer,
      kafkaProducer,
      routesCreator
    )

    val endpoints = List(Endpoint("name", "url", 1))
    val httpRoutes = mock[HttpRoutes]
    val bindingFuture = mock[Future[Http.ServerBinding]]

    when(configLoader.loadEndpoints()).thenReturn(endpoints)
    when(routesCreator.apply(isEq(endpoints), any[ActorRef], any[ExecutionContext])).thenReturn(httpRoutes)

    when(httpRoutes.bind()(any[ActorSystem])).thenReturn(bindingFuture)

  }

  "Runner" must {

    "x" in new Test {

      run

    }

  }

}
