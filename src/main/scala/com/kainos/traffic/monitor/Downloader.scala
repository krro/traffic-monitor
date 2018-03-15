package com.kainos.traffic.monitor

import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import akka.util.ByteString

import scala.concurrent.Future

trait Downloader {

  def downloadEndpoint(endpointUrl: String)(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer): Future[String] = {
    import actorSystem.dispatcher

    println(endpointUrl)

    Http(actorSystem)
      .singleRequest(HttpRequest(uri = endpointUrl, method = HttpMethods.GET))
      .flatMap { httpResponse =>
        httpResponse
          .entity
          .withoutSizeLimit()
          .dataBytes
          .runFold(ByteString(Array[Byte]()))(_ ++ _)
          .map(_.decodeString(Charset.defaultCharset()))
      }
  }

}
