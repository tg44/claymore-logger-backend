package com.github.tg44.claymore.repository.measures

import java.security.InvalidParameterException

import com.github.tg44.claymore.config.Config
import com.github.tg44.claymore.repository.MongoDb
import org.mongodb.scala.{Completed, MongoCollection}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates.push
import scaldi.{Injectable, Injector}

import scala.concurrent.{ExecutionContextExecutor, Future}

class MeasureRepo(implicit injector: Injector, ec: ExecutionContextExecutor) extends Injectable {
  val mongoDb = inject[MongoDb]
  val config = inject[Config]

  val collection: MongoCollection[Measure] = mongoDb.database.getCollection[Measure](config.MONGO.measureCollection)

  def getMesuresInRange(userExtId: String, from: Long, to: Long): Future[Seq[Measure]] = {
    require(from <= to)
    collection.find(and(equal("userExtId", userExtId), and(lte("fromTimeStamp", to), gte("toTimeStamp", from)))).toFuture
  }

  def saveNewMeasure(userExtId: String, data: StatisticData, period: (Long, Long)): Future[Completed] = {
    require(period._1 <= period._2)
    require(data.timeStamp >= period._1 && data.timeStamp <= period._2)
    getMesuresInRange(userExtId, period._1, period._2).flatMap { list =>
      if (list.size == 1) {
        collection.updateOne(equal("_id", list.head._id), push("data", data)).toFuture.map(_ => Completed())
      } else if (list.isEmpty) {
        val measure = Measure(userExtId, period._1, period._2, Seq(data))
        collection.insertOne(measure).toFuture
      } else {
        throw new InvalidParameterException("the given period is matching more then one existing document")
      }
    }
  }

}
