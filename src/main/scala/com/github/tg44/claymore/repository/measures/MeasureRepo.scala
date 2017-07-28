package com.github.tg44.claymore.repository.measures

import com.github.tg44.claymore.Config
import com.github.tg44.claymore.repository.MongoDb
import org.mongodb.scala.MongoCollection
import scaldi.{Injectable, Injector}

class MeasureRepo(implicit injector: Injector) extends Injectable {
  val mongoDb = inject[MongoDb]
  val collection: MongoCollection[Measure] = mongoDb.database.getCollection[Measure](Config.MONGO.measureCollection)

  def getMesuresInRange(userExtId: String, from: Long, to: Long) = ???

  def saveNewMeasure(userExtId: String, data: StatisticData) = ???

}
