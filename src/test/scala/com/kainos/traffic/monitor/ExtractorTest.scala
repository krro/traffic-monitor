package com.kainos.traffic.monitor

import org.scalatest.{Matchers, WordSpec}

class ExtractorTest extends WordSpec with Matchers {

  "Extractor" must {

    "extract all values" in {

      val extractor = new Extractor {}

      val json =
        """
          |{
          | "a": {
          |   "b": [{
          |     "id": 11
          |   }, {
          |     "id": 22,
          |     "ignore": true
          |   }, {
          |     "id": 44,
          |     "ignore": false
          |   }],
          |   "c": {
          |     "d": "33"
          |   }
          | }
          |}
        """.stripMargin

      val toExtract = List(Extraction("b", "$.a.b[?(!(@.ignore == true))].id"), Extraction("d", "$.a.c.d"))

      val expected = Map("b" -> List("11", "44"), "d" -> List("33"))

      extractor.extract(json, toExtract) shouldEqual(expected)
    }

  }



}
