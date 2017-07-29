package com.github.tg44.claymore.repository.users

import com.github.tg44.claymore.Config
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
  val collection: MongoCollection[User] = mongoDb.database.getCollection[User](Config.MONGO.userCollection)

  def findUserByExtId(extId: String): Future[Option[User]] = {
    collection.find(equal("extid", extId)).limit(2).toFuture.map(list => if (list.size == 1) Option(list.head) else None)
  }

  def findUserByApiKey(apiKey: String): Future[Option[User]] = {
    collection.find(equal("keys.value", apiKey)).limit(2).toFuture.map(list => if (list.size == 1) Option(list.head) else None)
  }

  def insertNewUser(usr: User): Future[Completed] = {
    collection.insertOne(usr).toFuture
  }

  def generateNewApiKeyToUser(extId: String, keyName: String): Future[Option[ApiKey]] = {
    val key = ApiKey(keyName, uuid, nowInUnix)
    collection.updateOne(equal("extid", extId), push("keys", key)).toFuture.map(x => if (x.getMatchedCount == 1) Option(key) else None)
  }

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
