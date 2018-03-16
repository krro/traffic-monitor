package com.kainos.traffic.monitor

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._

class DownloaderTest extends TestKit(ActorSystem("status-test")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  implicit val materializer = ActorMaterializer()

  "Downloader must" must {

    "download url" in {

      import system.dispatcher

      val helloWorld = "hello world"

      val url = "http://localhost:8081/api/test"

      val status = TestProbe()

      val testRoute: Route = path("api" / "test") {
        status.expectMsg(Status.DownloadStart(url))
        complete(helloWorld)
      }

      val endpoint = Endpoint("test", url, 1)

      val downloader = new Downloader {}

      val binding = Http(system).bindAndHandle(testRoute, "localhost", 8081)

      val futureResult = binding.flatMap {
        case _ => downloader.downloadEndpoint(endpoint, status.ref)
      }

      futureResult.onComplete {
        case _ => binding.flatMap(_.unbind())
      }

      val result = Await.result(futureResult, 1 second)

      status.expectMsg(Status.DownloadEnd(url))

      result shouldEqual helloWorld
    }

  }
}