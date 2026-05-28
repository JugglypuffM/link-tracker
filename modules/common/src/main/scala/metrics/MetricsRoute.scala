package metrics

import cats.effect.IO
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.*

import java.io.ByteArrayOutputStream

object MetricsRoute {
  def apply(registry: PrometheusRegistry): HttpRoutes[IO] = {
    val writer = new PrometheusTextFormatWriter(false)

    HttpRoutes.of[IO] {
      case GET -> Root / "metrics" =>
        IO {
          val outputStream = new ByteArrayOutputStream()
          val snapshots = registry.scrape()
          writer.write(outputStream, snapshots)
          outputStream.toString("UTF-8")
        }.flatMap { m =>
          Ok(m).map(_.withContentType(`Content-Type`(MediaType.text.plain)))
        }
    }
  }
}
