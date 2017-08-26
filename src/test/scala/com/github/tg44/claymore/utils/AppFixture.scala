package com.github.tg44.claymore.utils

import java.util.logging.{Level, Logger}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.simplyscala.MongoEmbedDatabase
import com.github.tg44.claymore.config.{ClientConfig, JwtProperties, MongoConfig, Server}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object AppFixture extends MongoEmbedDatabase {

  val dbTimeout = 3 seconds

  var portCounter = 20000

  val defaultServerConfig = Server("localhost", 3000, true, JwtProperties("test", 50000))
  val defaultClientConfig = ClientConfig(5000)

  def withMongoDb(serverConfig: Server = defaultServerConfig, clientConfig: ClientConfig = defaultClientConfig)(
      block: TestModule => Unit
  )(implicit ec: ExecutionContextExecutor, system: ActorSystem, materializer: ActorMaterializer): Unit = {
    //TODO: this is not so safe...
    portCounter += 1
    val port = portCounter
    val mongodProps = mongoStart(port)
    val mongoConfig = MongoConfig(s"mongodb://localhost:$port", "test", "users", "measures")
    implicit val modules = new TestModule(serverConfig, mongoConfig, clientConfig)

    val mongoLogger = Logger.getLogger("org.mongodb.driver")
    mongoLogger.setLevel(Level.SEVERE)

    try { block(modules) } finally { Option(mongodProps).foreach(mongoStop) }
  }

}
