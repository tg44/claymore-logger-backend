package com.github.tg44.claymore.repository.users

import com.github.tg44.claymore.config.Config
import com.github.tg44.claymore.repository.MongoDb
import org.mongodb.scala.{Completed, MongoCollection}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import scaldi.{Injectable, Injector}

import scala.concurrent.{ExecutionContextExecutor, Future}
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.bson.collection.immutable.Document

class UserRepo(implicit injector: Injector, ec: ExecutionContextExecutor) extends Injectable {
  import com.github.tg44.claymore.utils.GeneralUtil._

  val mongoDb = inject[MongoDb]
  val config = inject[Config]
  val collection: MongoCollection[User] = mongoDb.database.getCollection[User](config.MONGO.userCollection)

  def findUserByExtId(extId: String): Future[Option[User]] = {
    collection.find(equal("extid", extId)).limit(2).toFuture.map(list => if (list.size == 1) Option(list.head) else None)
  }

  def findKeyBySecret(apiKey: String): Future[Option[ApiKey]] = {
    collection.find(equal("keys.value", apiKey)).limit(1).toFuture.map(list => if (list.size == 1) list.head.keys.find(key => key.value == apiKey) else None)
  }

  private def checkUserWithKey(extId: String, apiKey: String): Future[Boolean] = {
    collection.find(and(equal("keys.value", apiKey), equal("extid", extId))).limit(1).toFuture().map(_.size == 1)
  }

  def insertNewUser(usr: User): Future[Completed] = {
    collection.insertOne(usr).toFuture
  }

  def generateNewApiKeyToUser(extId: String, keyName: String): Future[Option[ApiKey]] = {
    val key = ApiKey(keyName, uuid, extId, nowInUnix)
    collection.updateOne(equal("extid", extId), push("keys", key)).toFuture.map(x => if (x.getMatchedCount == 1) Option(key) else None)
  }

  def importApiKey(fromExtId: String, toExtId: String, keyName: String, keySecret: String): Future[Option[Completed]] = {
    val key = ApiKey(keyName, keySecret, fromExtId, nowInUnix)
    checkUserWithKey(fromExtId, keySecret).flatMap { exist =>
      if (exist)
        collection.updateOne(equal("extid", toExtId), push("keys", key)).toFuture.map(x => if (x.getMatchedCount == 1) Option(Completed()) else None)
      else
        Future.successful(None)
    }
  }

  //todo need to handle if the extid is the creator
  def deleteApiKey(extId: String, keyValue: String): Future[Option[Completed]] = {
    collection
      .updateOne(
        equal("extid", extId),
        Document("$pull" -> Document("keys" -> Document("value" -> BsonString(keyValue))))
      )
      .toFuture
      .map(x => if (x.getMatchedCount == 1) Option(Completed()) else None)
  }

}
