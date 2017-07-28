package com.github.tg44.claymore.repository

import com.github.tg44.claymore.Config
import com.github.tg44.claymore.repository.measures.Measure
import com.github.tg44.claymore.repository.users.User
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

class MongoDb {
  val codecRegistry = fromRegistries(
    fromProviders(classOf[User]),
    fromProviders(classOf[Measure]),
    DEFAULT_CODEC_REGISTRY
  )

  val mongoClient: MongoClient = MongoClient(Config.MONGO.url)
  val database: MongoDatabase = mongoClient.getDatabase(Config.MONGO.database).withCodecRegistry(codecRegistry)
}
