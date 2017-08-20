package com.github.tg44.claymore.api.data

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.tg44.claymore.repository.measures.{CardStatistic, CurrencyInformation, StatisticData}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait StatDataJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {


  implicit val currencyInformationJsonFormatter: RootJsonFormat[CurrencyInformation] = jsonFormat8(CurrencyInformation)
  implicit val cardStatisticsJsonFormatter: RootJsonFormat[CardStatistic] = jsonFormat3(CardStatistic)
  implicit val statDataJsonFormatter: RootJsonFormat[StatisticData] = jsonFormat10(StatisticData)
  implicit val currencyDataRespJsonFormatter: RootJsonFormat[CurrencyData] = jsonFormat6(CurrencyData.apply)
  implicit val statDataRespJsonFormatter: RootJsonFormat[StatisticDataresponseDto] = jsonFormat5(StatisticDataresponseDto.apply)
}
