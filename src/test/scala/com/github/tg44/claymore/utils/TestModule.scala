package com.github.tg44.claymore.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tg44.claymore.Main.MainModule.binding
import com.github.tg44.claymore.api.google.GoogleAuthApi
import com.github.tg44.claymore.api.keyhandling.KeyHandlerApi
import com.github.tg44.claymore.api.service.ServiceApi
import com.github.tg44.claymore.config._
import com.github.tg44.claymore.jwt.Jwt
import com.github.tg44.claymore.repository.MongoDbImpl
import com.github.tg44.claymore.repository.measures.MeasureRepo
import com.github.tg44.claymore.repository.users.UserRepo
import com.github.tg44.claymore.service.{AuthService, StatisticDataService}
import scaldi.Module

import scala.concurrent.ExecutionContextExecutor

class TestModule(serverConfig: Server, mongoConfig: MongoConfig, clientConfig: ClientConfig)(implicit ec: ExecutionContextExecutor,
                                                                                             system: ActorSystem,
                                                                                             materializer: ActorMaterializer)
    extends Module {
  binding toProvider new TestConfigImpl(serverConfig, mongoConfig, clientConfig)
  binding toProvider new Jwt

  binding toProvider new MongoDbImpl
  binding toProvider new UserRepo
  binding toProvider new MeasureRepo

  binding toProvider new ServiceApi
  binding toProvider new GoogleAuthApi
  binding toProvider new KeyHandlerApi

  binding toProvider new AuthService
  binding toProvider new StatisticDataService

}

class TestConfigImpl(serverConfig: Server, mongoConfig: MongoConfig, clientConfig: ClientConfig) extends Config {
  val SERVER: Server = serverConfig
  val MONGO: MongoConfig = mongoConfig
  val CLIENT: ClientConfig = clientConfig
  val GOOGLE: GoogleAuthConfig = GoogleAuthConfig("", "", "", "")
}
