package com.github.tg44.claymore

package object config {
  case class Server(url: String, port: Int, needAuth: Boolean, jwt: JwtProperties)
  case class JwtProperties(secret: String, exp: Long)

  case class ClientConfig(defaultWaitTimeInSecs: Long)

  case class MongoConfig(url: String, database: String, userCollection: String, measureCollection: String)
}
