package com.kainos.traffic.monitor

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.Sink
import org.apache.commons.codec.digest.DigestUtils
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import spray.json._

import scala.concurrent.Future

case class KafkaMsg(name: String, timestamp: Long, content: String, hash: String)

trait KafkaProducer extends DefaultJsonProtocol {

  type ProducerMessage = ProducerRecord[String, String]
  type ProducerSink = Sink[ProducerMessage, Future[Done]]

  implicit val kafkaFormat = jsonFormat4(KafkaMsg)

  def kafkaProducer(implicit system: ActorSystem): ProducerSink = {
    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
    Producer
      .plainSink(producerSettings)
//      .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
  }

  def createRecord(endpoint: Endpoint, content: String): ProducerMessage = {
    val hash = DigestUtils.md5Hex(content)
    val message = KafkaMsg(endpoint.name, System.currentTimeMillis(), content, hash)
    new ProducerRecord[String, String](endpoint.name, PrettyPrinter(message.toJson))
  }

}
