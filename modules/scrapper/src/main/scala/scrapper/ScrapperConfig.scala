package scrapper

import pureconfig.ConfigReader

final case class ScrapperConfig(
    linkProcessMilliseconds: Int,
    checkIntervalSeconds: Int,
    batchSize: Int
) derives ConfigReader
