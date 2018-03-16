package com.kainos.traffic.monitor

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import org.scalatest.{Matchers, WordSpec}

class HttpRoutesTest extends WordSpec with Matchers with ScalatestRouteTest {

  trait Test {

    val endpoints = List(
      Endpoint("endpoint1", "url1", 30),
      Endpoint("endpoint2", "url2", 10))

    val currentKeys = List("url3", "url4")

    val statusActor = TestProbe()

    val httpRoutes = new HttpRoutes(endpoints, statusActor.ref)

    val request = Get("/api/status") ~> httpRoutes.routes

    statusActor.expectMsg(Status.GetKeys)
    statusActor.reply(Status.Keys(currentKeys))

  }

  "/status endpoint" must {

    "include endpoints" in new Test {

      import httpRoutes._

      request ~> check {
        handled shouldEqual(true)
        status shouldEqual(StatusCodes.OK)
        responseAs[StatusMsg].endpoints shouldEqual(endpoints)
      }

    }

    "ask status actor for current tasks " in new Test {

      import httpRoutes._

      request ~> check {
        handled shouldEqual(true)
        status shouldEqual(StatusCodes.OK)
        responseAs[StatusMsg].currentTasks shouldEqual(currentKeys)
      }

    }

  }

}
