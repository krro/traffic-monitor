package com.kainos.traffic.monitor

import akka.stream.scaladsl.{Merge, Source}

trait Utils {

  def combine[T](sources: List[Source[T, _]]): Source[T, _] = {
    Source.combine(Source.empty[T], Source.empty[T], sources: _*)(Merge(_))
  }

}
