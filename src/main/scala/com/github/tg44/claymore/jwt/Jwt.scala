package com.github.tg44.claymore.jwt

import pdi.jwt.{JwtAlgorithm, JwtJson4s}
import akka.http.scaladsl.server.Directives.{headerValueByName, pass, provide, reject}
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive0, Directive1}
import com.github.tg44.claymore.config.Config
import scaldi.{Injectable, Injector}

import scala.util.{Failure, Success, Try}

class Jwt(implicit injector: Injector) extends Injectable {
  import org.json4s._
  import org.json4s.native.JsonMethods._

  private[this] val algorithm = JwtAlgorithm.HS512

  private[this] val JWT_HEADER_NAME = "Authorization"

  val config = inject[Config]

  lazy val skipThis = !config.SERVER.needAuth
  lazy val jwtSecret = config.SERVER.jwt.secret

  def encode[T <: AnyRef](claim: T)(implicit formats: Formats): String = {
    import org.json4s.native.Serialization.write
    JwtJson4s.encode(claim = write[T](claim), key = jwtSecret, algorithm = algorithm)
  }

  def encode(claim: JObject): String =
    JwtJson4s.encode(claim = claim, key = jwtSecret, algorithm = algorithm)

  def decode[T](token: String)(implicit formats: Formats, mf: scala.reflect.Manifest[T]): Try[T] =
    JwtJson4s
      .decode(token = token, key = jwtSecret, algorithms = Seq(algorithm))
      .map { jwtClaim =>
        parse(jwtClaim.content).extract[T]
      }

  private[this] def validateAuthority(payload: JwtPayload, authority: String): Directive1[JwtPayload] =
    if (payload.authorities.contains(authority))
      provide(payload)
    else
      reject(AuthorizationFailedRejection)

  private[this] def extractAuthorizationHeader: Directive1[String] =
    if (skipThis) {
      provide("Bearer " + encode(godJwtPayload))
    } else {
      headerValueByName(JWT_HEADER_NAME)
    }

  private[this] def validateToken(token: String): Directive1[JwtPayload] = {
    if (token.startsWith("Bearer ")) {
      decode[JwtPayload](token.split(" ")(1)) match {
        case Success(payload) => provide(payload)
        case Failure(_) => reject(AuthorizationFailedRejection)
      }
    } else {
      reject(AuthorizationFailedRejection)
    }
  }

  def authenticatedWithData: Directive1[JwtPayload] =
    extractAuthorizationHeader
      .flatMap(validateToken)

  def checkAuthority(authority: String): Directive1[JwtPayload] =
    extractAuthorizationHeader
      .flatMap(validateToken)
      .flatMap(payload => validateAuthority(payload, authority))
}
