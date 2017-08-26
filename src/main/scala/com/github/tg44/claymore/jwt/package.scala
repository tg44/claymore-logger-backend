package com.github.tg44.claymore

package object jwt {
  import org.json4s.DefaultFormats

  implicit val jsonFormats = DefaultFormats

  val godJwtPayload = JwtPayload("god")
  val godJwtServicePayload = JwtServicePayload("god")
}
