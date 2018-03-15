package com.kainos.traffic.monitor

import akka.stream.scaladsl.{Merge, Source}

trait Utils {

  def combine(sources: List[Source[Endpoint, _]]): Source[Endpoint, _] = {
    Source.combine(Source.empty[Endpoint], Source.empty[Endpoint], sources: _*)(Merge(_))
  }

}
