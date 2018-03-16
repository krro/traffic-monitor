package com.kainos.traffic.monitor

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.kainos.traffic.monitor.Status.Keys
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class StatusTest extends TestKit(ActorSystem("status-test")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Status" must {

    "keep track of current downloads" in {

      val status = system.actorOf(Props[Status])

      status ! Status.GetKeys

      expectMsg(Keys(Nil))

      status ! Status.DownloadStart("url1")
      status ! Status.DownloadStart("url2")
      status ! Status.DownloadStart("url3")

      status ! Status.GetKeys

      expectMsg(Keys(List("url1", "url2", "url3")))

      status ! Status.DownloadEnd("url1")
      status ! Status.DownloadStart("url4")
      status ! Status.DownloadEnd("url3")

      status ! Status.GetKeys

      expectMsg(Keys(List("url2", "url4")))
    }

  }



}
