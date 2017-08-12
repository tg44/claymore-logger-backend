package com.github.tg44.claymore.api.google

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait GoogleJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  import org.json4s.DefaultFormats
  implicit val jsonFormats = DefaultFormats

  implicit val apiSecurityDtoJsonFormatter: RootJsonFormat[ApiSecurityDto] = jsonFormat1(ApiSecurityDto)
  implicit val apiSecurityAnsDtoJsonFormatter: RootJsonFormat[ApiSecurityAnswerDto] = jsonFormat1(ApiSecurityAnswerDto)

}
