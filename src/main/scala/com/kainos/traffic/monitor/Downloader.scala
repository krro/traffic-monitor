package com.kainos.traffic.monitor

import java.nio.charset.Charset

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.kainos.traffic.monitor.Status.{DownloadEnd, DownloadStart}

import scala.concurrent.Future

trait Downloader {

  def downloadEndpoint(endpoint: Endpoint, statusActor: ActorRef)(implicit actorSystem: ActorSystem): Future[String] = {
    import actorSystem.dispatcher

    statusActor ! DownloadStart(endpoint.url)

    val futureResult = downloadEndpoint(endpoint.url)

    futureResult.onComplete {
      case _ => statusActor ! DownloadEnd(endpoint.url)
    }

    futureResult
  }

  private def downloadEndpoint(url: String)(implicit actorSystem: ActorSystem): Future[String] = {
    implicit val materializer = ActorMaterializer()
    import actorSystem.dispatcher

    Http(actorSystem)
      .singleRequest(HttpRequest(uri = url, method = HttpMethods.GET))
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
