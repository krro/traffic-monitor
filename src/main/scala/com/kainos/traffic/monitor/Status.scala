package com.kainos.traffic.monitor

import akka.actor.Actor
import com.kainos.traffic.monitor.Status.{DownloadEnd, DownloadStart, GetKeys, Keys}

object Status {

  case class DownloadStart(key: String)
  case class DownloadEnd(key: String)

  case object GetKeys
  case class Keys(keys: List[String])
}

class Status extends Actor {

  var currentTasks: List[String] = Nil

  override def receive: Receive = {

    case DownloadStart(key) => currentTasks = key :: currentTasks

    case DownloadEnd(key) => currentTasks = currentTasks.filterNot(_ == key)

    case GetKeys => sender ! Keys(currentTasks)

  }

}
