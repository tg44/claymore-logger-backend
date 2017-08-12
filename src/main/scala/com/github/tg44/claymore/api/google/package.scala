package com.github.tg44.claymore.api

package object google {
  case class ApiSecurityDto(
      secret: String
  )
  case class ApiSecurityAnswerDto(
      jwt: String
  )
}
