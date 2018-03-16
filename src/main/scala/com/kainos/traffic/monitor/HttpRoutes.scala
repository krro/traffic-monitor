package com.kainos.traffic.monitor

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.kainos.traffic.monitor.Status.{GetKeys, Keys}
import spray.json.{DefaultJsonProtocol, PrettyPrinter}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

case class StatusMsg(endpoints: List[Endpoint], currentTask: List[String])

class HttpRoutes(endpoints: List[Endpoint], statusActor: ActorRef)(implicit executionContext: ExecutionContext) extends DefaultJsonProtocol with SprayJsonSupport {

  implicit val paramFormat = jsonFormat4(Param)
  implicit val extractionFormat = jsonFormat2(Extraction)
  implicit val endpointFormat = jsonFormat8(Endpoint)
  implicit val statusFormat = jsonFormat2(StatusMsg)
  implicit val printer = PrettyPrinter

  val routes: Route = path("api" / "status") {
    complete {
      implicit val timeout = Timeout(5 seconds)
      (statusActor ? GetKeys).mapTo[Keys].map { keys =>
        StatusMsg(endpoints, keys.keys)
      }
    }
  }

}
