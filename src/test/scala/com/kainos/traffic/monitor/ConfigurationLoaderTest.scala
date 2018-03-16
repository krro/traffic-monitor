package com.kainos.traffic.monitor

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{Matchers, WordSpec}

class ConfigurationLoaderTest extends WordSpec with Matchers {

  trait Test {

    def testConfig: Config

    val loader = new ConfigurationLoader {
      override def config: Config = testConfig
    }

  }

  "Config loader" must {

    "load endpoints" in new Test {

      def testConfig = ConfigFactory.parseString(
        """
          |
          |monitor.endpoints = [
          |    {
          |      name: "master",
          |      url: "master-url",
          |      intervalSeconds: 20,
          |      extract: [{
          |        name: "id"
          |        path: "$.some.path"
          |      }]
          |      trigger: "detail"
          |    },
          |    {
          |      name: "detail",
          |      url: "detail-url",
          |      intervalSeconds: 10,
          |      params: [
          |        {
          |          name: "id",
          |          type: "date",
          |          values: ["today", "yesterday"],
          |          format: "YYYY-MM-dd"
          |        }
          |      ],
          |      inner: true
          |    }
          |]
        """.stripMargin)

      val loaded = loader.loadEndpoints()

      val expected = List(
        Endpoint("master", "master-url", 20, Some("detail"), List(Extraction("id", "$.some.path")), Nil, false, false),
        Endpoint("detail", "detail-url", 10, None, Nil, List(Param("id", "date", List("today", "yesterday"), Some("YYYY-MM-dd"))), true, false)
      )

      loaded shouldEqual(expected)
    }

  }

}
