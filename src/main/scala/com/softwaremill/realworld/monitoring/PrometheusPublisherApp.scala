package com.softwaremill.realworld.monitoring

import zio.ZIO
import zio.http.{Method, Response, Routes, handler}
import zio.metrics.connectors.prometheus.PrometheusPublisher

object PrometheusPublisherApp:
  def apply(): Routes[PrometheusPublisher, Nothing] =
    Routes(
      Method.GET / "metrics" -> handler(
        ZIO.serviceWithZIO[PrometheusPublisher](_.get.map(Response.text))
      )
    )
