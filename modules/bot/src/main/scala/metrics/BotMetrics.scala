package metrics

import cats.effect.IO
import io.prometheus.metrics.core.metrics.{Counter, Histogram}
import io.prometheus.metrics.model.registry.PrometheusRegistry

import scala.concurrent.duration.FiniteDuration

trait BotMetrics[F[_]] {
  def notificationCompleted(status: String): F[Unit]
  def deliveryDuration(status: String, duration: FiniteDuration): F[Unit]
}

object BotMetrics {
  def make(registry: PrometheusRegistry): IO[BotMetrics[IO]] = IO {
    val notificationsTotal = Counter.builder()
      .name("bot_notifications_total")
      .help("Total number of notifications sent by bot")
      .labelNames("status")
      .register(registry)

    val deliveryDuration = Histogram.builder()
      .name("bot_delivery_duration_seconds")
      .help("Duration of notification delivery in seconds")
      .labelNames("status")
      .register(registry)

    new Impl(notificationsTotal, deliveryDuration)
  }

  private class Impl(
      notificationsTotal: Counter,
      deliveryDuration: Histogram
  ) extends BotMetrics[IO] {

    override def notificationCompleted(status: String): IO[Unit] =
      IO(notificationsTotal.labelValues(status).inc())

    override def deliveryDuration(status: String, duration: FiniteDuration): IO[Unit] =
      IO(deliveryDuration.labelValues(status).observe(duration.toSeconds.toDouble))
  }
}
