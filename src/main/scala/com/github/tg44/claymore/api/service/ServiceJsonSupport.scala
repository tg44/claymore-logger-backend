package com.github.tg44.claymore.api.service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.tg44.claymore.repository.measures.{CardStatistic, CurrencyInformation}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait ServiceJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  import org.json4s.DefaultFormats
  implicit val jsonFormats = DefaultFormats

  implicit val cardStatisticsJsonFormatter: RootJsonFormat[CardStatistic] = jsonFormat4(CardStatistic)
  implicit val currencyInformationJsonFormatter: RootJsonFormat[CurrencyInformation] = jsonFormat8(CurrencyInformation)
  implicit val parsedStatisticResponseJsonFormatter: RootJsonFormat[ParsedStatisticResponse] = jsonFormat6(ParsedStatisticResponse)

  implicit val statisticDataDtoJsonFormatter: RootJsonFormat[StatisticDataDto] = jsonFormat4(StatisticDataDto)
  implicit val apiSecurityDtoJsonFormatter: RootJsonFormat[ApiSecurityDto] = jsonFormat1(ApiSecurityDto)
}
