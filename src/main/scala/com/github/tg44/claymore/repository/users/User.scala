package com.github.tg44.claymore.repository.users

import org.mongodb.scala.bson.ObjectId

object User {
  def apply(extid: String, email: String, keys: Seq[ApiKey]): User = {
    User(new ObjectId(), extid, email, keys)
  }
}

case class User(_id: ObjectId, extid: String, email: String, keys: Seq[ApiKey])
case class ApiKey(name: String, value: String, createdAt: Long)
