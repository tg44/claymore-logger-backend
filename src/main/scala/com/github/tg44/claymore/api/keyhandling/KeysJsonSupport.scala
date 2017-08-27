package com.github.tg44.claymore.api.keyhandling

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.tg44.claymore.repository.users.ApiKey
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait KeysJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  import org.json4s.DefaultFormats
  implicit val jsonFormats = DefaultFormats

  implicit val apiKeyJsonFormatter: RootJsonFormat[ApiKey] = jsonFormat4(ApiKey)
  implicit val apiKeyListJsonFormatter: RootJsonFormat[ApiKeyList] = jsonFormat1(ApiKeyList)
  implicit val nameJsonFormatter: RootJsonFormat[Name] = jsonFormat1(Name)
  implicit val importKeyJsonFormatter: RootJsonFormat[ImportKey] = jsonFormat3(ImportKey)

}
