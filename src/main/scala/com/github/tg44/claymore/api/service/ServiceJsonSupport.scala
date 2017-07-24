package com.github.tg44.claymore.api.service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait ServiceJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val cardStatisticsJsonFormatter: RootJsonFormat[CardStatistic] = jsonFormat4(CardStatistic)
  implicit val currencyInformationJsonFormatter: RootJsonFormat[CurrencyInformation] = jsonFormat7(CurrencyInformation)
  implicit val parsedStatisticResponseJsonFormatter: RootJsonFormat[ParsedStatisticResponse] = jsonFormat7(ParsedStatisticResponse)

  implicit val statisticDataDtoJsonFormatter: RootJsonFormat[StatisticDataDto] = jsonFormat4(StatisticDataDto)
  implicit val apiSecurityDtoJsonFormatter: RootJsonFormat[ApiSecurityDto] = jsonFormat1(ApiSecurityDto)
}
