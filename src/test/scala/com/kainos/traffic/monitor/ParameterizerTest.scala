package com.kainos.traffic.monitor

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.scalatest.{Matchers, WordSpec}

class ParameterizerTest extends WordSpec with Matchers{

  "Parameterizer" must {

    "parameterize" in {

      val parameterizer = new Parameterizer {}

      val datePattern = "YYYY-MM-dd"

      val endpoint = Endpoint("", "/{param1}/{param2}/{param3}", 1, params = List(
        Param("param1", "extracted"),
        Param("param2", "enumerated", values = List("1", "2")),
        Param("param3", "date", format = Some(datePattern), values = List("today", "yesterday"))))

      val extractions = Map("param1" -> List("a", "b"))

      val dateFormatter = DateTimeFormatter.ofPattern(datePattern)
      val today = dateFormatter.format(LocalDateTime.now())
      val yesterday = dateFormatter.format(LocalDateTime.now().minusDays(1))

      val expectedEndpoints = List(
        endpoint.copy(url = s"/a/1/${today}"),
        endpoint.copy(url = s"/a/1/${yesterday}"),
        endpoint.copy(url = s"/a/2/${today}"),
        endpoint.copy(url = s"/a/2/${yesterday}"),
        endpoint.copy(url = s"/b/1/${today}"),
        endpoint.copy(url = s"/b/1/${yesterday}"),
        endpoint.copy(url = s"/b/2/${today}"),
        endpoint.copy(url = s"/b/2/${yesterday}")
      )

      parameterizer.parameterize(endpoint, extractions) shouldEqual(expectedEndpoints)
    }

  }



}
