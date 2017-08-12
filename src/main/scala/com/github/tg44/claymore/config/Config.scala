package com.github.tg44.claymore.config

import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}

trait Config {
  val SERVER: Server
  val MONGO: MongoConfig
  val CLIENT: ClientConfig
  val GOOGLE: GoogleAuthConfig
  val SPENDPOINTS: SinglePageURLs
}

class ConfigImpl extends Config {

  private[this] implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  import pureconfig.loadConfigOrThrow

  lazy val SERVER: Server = loadConfigOrThrow[Server]("endpoint")
  lazy val MONGO: MongoConfig = loadConfigOrThrow[MongoConfig]("mongo")
  lazy val CLIENT: ClientConfig = loadConfigOrThrow[ClientConfig]("client")
  lazy val GOOGLE: GoogleAuthConfig = loadConfigOrThrow[GoogleAuthConfig]("googleAuth")
  lazy val SPENDPOINTS: SinglePageURLs = loadConfigOrThrow[SinglePageURLs]("singlePageUrls")
}
