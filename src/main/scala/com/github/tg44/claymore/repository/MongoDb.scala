package com.github.tg44.claymore.repository

import com.github.tg44.claymore.config.Config
import com.github.tg44.claymore.repository.measures.{CardStatistic, CurrencyInformation, Measure, StatisticData}
import com.github.tg44.claymore.repository.users.{ApiKey, User}
import com.mongodb.ConnectionString
import com.mongodb.async.client.MongoClientSettings
import com.mongodb.connection.SslSettings
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.connection._
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

  val connectionString = new ConnectionString(config.MONGO.url)

  val builder = MongoClientSettings.builder()
    .clusterSettings(ClusterSettings.builder().applyConnectionString(connectionString).build())
    .connectionPoolSettings(ConnectionPoolSettings.builder().applyConnectionString(connectionString).build())
    .serverSettings(ServerSettings.builder().build()).credentialList(connectionString.getCredentialList)
    .sslSettings(SslSettings.builder().applyConnectionString(connectionString).build())
    .streamFactoryFactory(NettyStreamFactoryFactory())
    .socketSettings(SocketSettings.builder().applyConnectionString(connectionString).build())

  if (connectionString.getReadPreference != null) builder.readPreference(connectionString.getReadPreference)
  if (connectionString.getReadConcern != null) builder.readConcern(connectionString.getReadConcern)
  if (connectionString.getWriteConcern != null) builder.writeConcern(connectionString.getWriteConcern)


  val mongoClient: MongoClient = MongoClient(builder.build())
  val database: MongoDatabase = mongoClient.getDatabase(config.MONGO.database).withCodecRegistry(codecRegistry)
}
