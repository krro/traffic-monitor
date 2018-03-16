package com.kainos.traffic.monitor

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Future
import scala.concurrent.duration._

class RequestSourceCreatorTest extends TestKit(ActorSystem("status-test")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  "Request Source" must {

    "create simple source" in {
      import system.dispatcher
      import akka.pattern.pipe
      implicit val materializer = ActorMaterializer()

      val sourceCreator = new RequestSourceCreator {}

      val endpoint = Endpoint("name", "url", 1)

      val endpoints = List(endpoint)

      val source = sourceCreator.createEndpointDownloadEventSource(endpoints, new sourceCreator.Ops(
        e => Future.successful("content"),
        (e, _) => Map.empty,
        (e, _) => List(e)
      ))

      val probe = TestProbe()

      source
        .takeWithin(5 seconds)
        .runWith(Sink.seq)
        .pipeTo(probe.ref)

      probe.expectMsgPF(10 seconds) {
        case seq: Seq[Endpoint] => seq.size >= 4 && seq.distinct == Seq(endpoint)
        case _ => false
      } shouldEqual(true)
    }

    "create complex source" in {
      import system.dispatcher
      import akka.pattern.pipe
      implicit val materializer = ActorMaterializer()

      val sourceCreator = new RequestSourceCreator {}

      val extractions = Map("param" -> List("value1", "value2"))

      val endpointMaster = Endpoint("master", "url-master", 1, trigger = Some("detail"))
      val endpointDetail = Endpoint("detail", "url-detail", 2, inner = true)

      val endpointDetail1 = endpointDetail.copy(url = "url1")
      val endpointDetail2 = endpointDetail.copy(url = "url2")

      val endpoints = List(endpointMaster, endpointDetail)

      val masterContent = "content"

      val source = sourceCreator.createEndpointDownloadEventSource(endpoints, new sourceCreator.Ops(
        e => Future.successful(masterContent),
        (e, _) => extractions,
        (e, _) => List(endpointDetail1, endpointDetail2)
      ))

      val probe = TestProbe()

      source
        .takeWithin(5 seconds)
        .runWith(Sink.seq)
        .pipeTo(probe.ref)

      probe.expectMsgPF(10 seconds) {
        case seq: Seq[Endpoint] => seq.size >= 4 && seq.distinct == Seq(endpointDetail1, endpointDetail2)
        case x => false
      } shouldEqual(true)
    }

  }


}
