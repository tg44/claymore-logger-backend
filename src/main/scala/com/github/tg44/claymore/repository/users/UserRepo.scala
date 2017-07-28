package com.github.tg44.claymore.repository.users

import com.github.tg44.claymore.Config
import com.github.tg44.claymore.repository.MongoDb
import org.mongodb.scala.{MongoCollection}
import org.mongodb.scala.model.Filters._
import scaldi.{Injectable, Injector}

import scala.concurrent.Future

class UserRepo(implicit injector: Injector) extends Injectable {
  val mongoDb = inject[MongoDb]
  val collection: MongoCollection[User] = mongoDb.database.getCollection[User](Config.MONGO.userCollection)

  def findUserByExtId(extId: String): Future[User] = {
    collection.find(equal("extid", extId)).first().toFuture()
  }

  def findUserByApiKey(apiKey: String): Future[User] = {
    collection.find(equal("keys.value", apiKey)).first().toFuture()
  }

  def insertNewUser(usr: User) = ???

  def generateNewApiKeyToUser(extId: String, keyName: String) = ???

  def deleteApiKey(extId: String, keyName: String) = ???

}
