package outbox

import pureconfig.ConfigReader

case class OutboxConfig(
    batchSize: Int,
    pollIntervalSeconds: Int
) derives ConfigReader
