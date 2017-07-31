package com.github.tg44.claymore.repository

import com.github.tg44.claymore.config.Config
import com.github.tg44.claymore.repository.measures.{CardStatistic, CurrencyInformation, Measure, StatisticData}
import com.github.tg44.claymore.repository.users.{ApiKey, User}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import scaldi.{Injectable, Injector}

trait MongoDb {
  val database: MongoDatabase

  val codecRegistry = fromRegistries(
    fromProviders(classOf[ApiKey]),
    fromProviders(classOf[User]),
    fromProviders(classOf[CardStatistic]),
    fromProviders(classOf[CurrencyInformation]),
    fromProviders(classOf[StatisticData]),
    fromProviders(classOf[Measure]),
    DEFAULT_CODEC_REGISTRY
  )
}

class MongoDbImpl(implicit injector: Injector) extends MongoDb with Injectable {

  private val config = inject[Config]

  val mongoClient: MongoClient = MongoClient(config.MONGO.url)
  val database: MongoDatabase = mongoClient.getDatabase(config.MONGO.database).withCodecRegistry(codecRegistry)
}
