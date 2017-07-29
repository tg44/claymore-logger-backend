package com.github.tg44.claymore.utils

import com.github.simplyscala.MongoEmbedDatabase
import com.github.tg44.claymore.Config
import com.github.tg44.claymore.Main.MainModule.binding
import com.github.tg44.claymore.api.service.ServiceApi
import com.github.tg44.claymore.repository.MongoDb
import com.github.tg44.claymore.repository.measures.{Measure, MeasureRepo}
import com.github.tg44.claymore.repository.users.{ApiKey, User, UserRepo}
import com.github.tg44.claymore.service.{AuthService, StatisticDataService}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import scaldi.Module

import scala.concurrent.ExecutionContextExecutor

class TestModule(port: Int)(implicit ec: ExecutionContextExecutor) extends Module {

  binding toProvider new TestMongoDbImpl(port)
  binding toProvider new UserRepo
  binding toProvider new MeasureRepo

  binding toProvider new ServiceApi

  binding toProvider new AuthService
  binding toProvider new StatisticDataService

}

class TestMongoDbImpl(port: Int) extends MongoDb {
  val mongoClient: MongoClient = MongoClient(s"mongodb://localhost:$port")
  val database: MongoDatabase = mongoClient.getDatabase(Config.MONGO.database).withCodecRegistry(codecRegistry)
}
