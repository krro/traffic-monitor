package com.kainos.traffic.monitor

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._
import spray.json._

class KafkaProducerTest extends TestKit(ActorSystem("status-test")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with EmbeddedKafka {

  "Kafka Producer" must {

    val producer = new KafkaProducer {
      override def kafkaSettings(system: ActorSystem): ProducerSettings[String, String] = super.kafkaSettings(system).withBootstrapServers("localhost:12345")
    }

    "send message to kafka" in {

      import akka.pattern.pipe
      import system.dispatcher
      implicit val materializer = ActorMaterializer()

      import producer._

      implicit val config = EmbeddedKafkaConfig(kafkaPort = 12345)

      withRunningKafka {

        val endpoint = Endpoint("topic", "url", 1)

        val json = "{}"

        val kafkaSink = producer.kafkaProducer

        val msg = producer.createRecord(endpoint, json)

        val complete = Source
          .apply(List(msg, msg, msg))
          .runWith(kafkaSink)

        Await.ready(complete, 3 seconds)

        consumeFirstStringMessageFrom("topic").parseJson.convertTo[KafkaMsg].content shouldEqual(json)
        consumeFirstStringMessageFrom("topic").parseJson.convertTo[KafkaMsg].content shouldEqual(json)
        consumeFirstStringMessageFrom("topic").parseJson.convertTo[KafkaMsg].content shouldEqual(json)
      }


    }

  }

}
