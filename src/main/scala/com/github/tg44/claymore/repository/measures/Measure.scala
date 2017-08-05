package com.github.tg44.claymore.repository.measures

import org.mongodb.scala.bson.ObjectId

object Measure {
  def apply(userExtId: String, fromTimeStamp: Long, toTimeStamp: Long, data: Seq[StatisticData]): Measure = {
    Measure(new ObjectId(), userExtId, fromTimeStamp, toTimeStamp, data)
  }
}

case class Measure(_id: ObjectId, userExtId: String, fromTimeStamp: Long, toTimeStamp: Long, data: Seq[StatisticData])

case class StatisticData(
    timeStamp: Long,
    endpointName: String,
    remoteAddress: String,
    remotePort: Int,
    minerVersion: String,
    runTimeInMins: Double,
    currencyInformations: Seq[CurrencyInformation],
    tempsPerCard: Seq[Double],
    fansPerCard: Seq[Double],
    cards: Seq[CardStatistic]
)

case class CurrencyInformation(
    currency: String,
    sumHR: Double,
    shares: Int,
    sharesRejected: Int,
    invalidShares: Int,
    poolSwitches: Int,
    perCardHR: Seq[Double],
    currentPool: String
)

case class CardStatistic(
    ethHR: Double,
    dcrHR: Double,
    temp: Double,
    fan: Double
)
