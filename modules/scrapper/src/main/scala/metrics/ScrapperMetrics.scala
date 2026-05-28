package metrics

import cats.effect.IO
import io.prometheus.metrics.core.metrics.{Counter, Histogram}
import io.prometheus.metrics.model.registry.PrometheusRegistry

import scala.concurrent.duration.FiniteDuration

trait ScrapperMetrics[F[_]] {
  def checkCompleted(status: String): F[Unit]
  def checkDuration(status: String, duration: FiniteDuration): F[Unit]
}

object ScrapperMetrics {
  def make(registry: PrometheusRegistry): IO[ScrapperMetrics[IO]] = IO {
    val checkTotal = Counter.builder()
      .name("scrapper_check_total")
      .help("Total number of link checks performed by scrapper")
      .labelNames("status")
      .register(registry)

    val checkDuration = Histogram.builder()
      .name("scrapper_check_duration_seconds")
      .help("Duration of link checks in seconds")
      .labelNames("status")
      .register(registry)

    new Impl(checkTotal, checkDuration)
  }

  private class Impl(
      checkTotal: Counter,
      checkDuration: Histogram
  ) extends ScrapperMetrics[IO] {

    override def checkCompleted(status: String): IO[Unit] =
      IO(checkTotal.labelValues(status).inc())

    override def checkDuration(status: String, duration: FiniteDuration): IO[Unit] =
      IO(checkDuration.labelValues(status).observe(duration.toSeconds.toDouble))
  }
}
