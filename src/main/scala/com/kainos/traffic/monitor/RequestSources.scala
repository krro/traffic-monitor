package com.kainos.traffic.monitor

import scala.concurrent.duration._
import akka.stream.scaladsl.Source

import scala.concurrent.{ExecutionContext, Future}

trait RequestSources extends Downloader with Utils {

  class Ops(val downloader: Endpoint => Future[String], val extractor: (String, List[Extraction]) => Map[String, List[String]], val parameterizer: (Endpoint, Map[String, List[String]]) => List[Endpoint])

  def createEndpointDownloadEventSource(endpoints: List[Endpoint], ops: Ops)(implicit executionContext: ExecutionContext): Source[Endpoint, _] = {
    combine(
      endpoints
      .filter(!_.inner)
      .map(createEndpointDownloadEventSource(_, endpoints, ops)))
  }

  private def createEndpointDownloadEventSource(endpoint: Endpoint, endpoints: List[Endpoint], ops: Ops)(implicit executionContext: ExecutionContext) = {
    endpoint.trigger.map { trigger =>
      val innerEndpoint = endpoints.filter(_.name == trigger).head
      createComplexSource(endpoint, innerEndpoint, ops)
    }.getOrElse(createSimpleTickSource(endpoint))
  }

  private def createSimpleTickSource(endpoint: Endpoint): Source[Endpoint, _] = {
    Source.tick(1 second, endpoint.interval seconds, endpoint)
  }

  private def createComplexSource(endpoint: Endpoint, innerEndpoint: Endpoint, ops: Ops)(implicit executionContext: ExecutionContext): Source[Endpoint, _] = {
    createSimpleTickSource(endpoint)
      .mapAsync(1)(e => ops.downloader(e).map(content => (e, content)))
      .flatMapConcat {
        case (_, content) => endpointsToTrigger(endpoint, content, innerEndpoint, ops)
      }
  }

  private def endpointsToTrigger(endpoint: Endpoint, content: String, endpointToTrigger: Endpoint, ops: Ops): Source[Endpoint, _] = {
    val extractions = ops.extractor(content, endpoint.extractions)

    val endpointsToRun = ops.parameterizer(endpointToTrigger, extractions)

    combine(
      endpointsToRun
      .map(createSimpleTickSource)
      .map(_.takeWithin(endpoint.interval seconds)))
  }
}
