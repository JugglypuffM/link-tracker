package config.kafka

import pureconfig.ConfigReader

case class KafkaConfig(topic: String, servers: String) derives ConfigReader
