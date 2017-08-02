package com.github.tg44.claymore.utils

import com.github.simplyscala.MongoEmbedDatabase
import com.github.tg44.claymore.Main.MainModule.binding
import com.github.tg44.claymore.api.service.ServiceApi
import com.github.tg44.claymore.config._
import com.github.tg44.claymore.jwt.Jwt
import com.github.tg44.claymore.repository.{MongoDb, MongoDbImpl}
import com.github.tg44.claymore.repository.measures.{Measure, MeasureRepo}
import com.github.tg44.claymore.repository.users.{ApiKey, User, UserRepo}
import com.github.tg44.claymore.service.{AuthService, StatisticDataService}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import scaldi.{Injectable, Injector, Module}

import scala.concurrent.ExecutionContextExecutor

class TestModule(serverConfig: Server, mongoConfig: MongoConfig, clientConfig: ClientConfig)(implicit ec: ExecutionContextExecutor) extends Module {
  binding toProvider new TestConfigImpl(serverConfig, mongoConfig, clientConfig)
  binding toProvider new Jwt

  binding toProvider new MongoDbImpl
  binding toProvider new UserRepo
  binding toProvider new MeasureRepo

  binding toProvider new ServiceApi

  binding toProvider new AuthService
  binding toProvider new StatisticDataService

}

class TestConfigImpl(serverConfig: Server, mongoConfig: MongoConfig, clientConfig: ClientConfig) extends Config {
  val SERVER: Server = serverConfig
  val MONGO: MongoConfig = mongoConfig
  val CLIENT: ClientConfig = clientConfig
}