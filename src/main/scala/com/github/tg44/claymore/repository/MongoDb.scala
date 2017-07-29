package com.github.tg44.claymore.repository

import com.github.tg44.claymore.Config
import com.github.tg44.claymore.repository.measures.Measure
import com.github.tg44.claymore.repository.users.{ApiKey, User}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

trait MongoDb {
  val database: MongoDatabase

  val codecRegistry = fromRegistries(
    fromProviders(classOf[ApiKey]),
    fromProviders(classOf[User]),
    fromProviders(classOf[Measure]),
    DEFAULT_CODEC_REGISTRY
  )
}

class MongoDbImpl extends MongoDb {
  val mongoClient: MongoClient = MongoClient(Config.MONGO.url)
  val database: MongoDatabase = mongoClient.getDatabase(Config.MONGO.database).withCodecRegistry(codecRegistry)
}
