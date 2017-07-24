package com.github.tg44.claymore

import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint}

object Config {

  case class Server(url: String, port: Int, needAuth: Boolean, jwt: JwtProperties)
  case class JwtProperties(secret: String, exp: Long)

  private[this] implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  import pureconfig.loadConfigOrThrow

  lazy val SERVER: Server = loadConfigOrThrow[Server]("endpoint")
}
