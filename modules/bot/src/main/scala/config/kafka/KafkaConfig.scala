package config.kafka

import pureconfig.ConfigReader

case class KafkaConfig(topic: String, servers: String, consumerGroup: String) derives ConfigReader
