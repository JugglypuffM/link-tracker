package config.resilience

import pureconfig.ConfigReader
import scala.concurrent.duration.*

final case class ResilienceConfig(
    timeout: FiniteDuration,
    maxRetries: Int,
    retryDelay: FiniteDuration,
    failureThreshold: Int,
    resetTimeout: FiniteDuration
) derives ConfigReader
