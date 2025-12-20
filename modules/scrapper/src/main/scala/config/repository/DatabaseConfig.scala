package config.repository

import pureconfig.ConfigReader

case class DatabaseConfig(
    url: String,
    username: String,
    password: String,
) derives ConfigReader
