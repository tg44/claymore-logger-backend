package com.github.tg44.claymore.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.github.tg44.claymore.config.Config
import com.github.tg44.claymore.jwt.{Jwt, JwtPayload, JwtServicePayload}
import com.github.tg44.claymore.repository.users.{ApiKey, User, UserRepo}
import com.github.tg44.claymore.service.AuthService.{AuthResponse, AuthServiceJsonSupport, UserInfo}
import com.google.common.cache.{Cache, CacheBuilder}
import java.util.concurrent.TimeUnit

import akka.util.ByteString
import com.github.tg44.claymore.utils.GeneralUtil
import org.mongodb.scala.Completed
import scaldi.{Injectable, Injector}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.{ExecutionContextExecutor, Future}

class AuthService(implicit injector: Injector, ec: ExecutionContextExecutor, system: ActorSystem, materializer: ActorMaterializer)
    extends Injectable
    with AuthServiceJsonSupport {

  implicit val jsonFormats2 = jsonFormats

  val userRepo = inject[UserRepo]
  val jwt = inject[Jwt]
  val config = inject[Config]

  private val authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth"
  private val tokenEndpoint = "https://www.googleapis.com/oauth2/v4/token"
  private val userInfoEndpoint = "https://www.googleapis.com/oauth2/v3/userinfo"

  private val cache: Cache[String, String] = CacheBuilder
    .newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build()

  def authenticateWithApiKey(secret: String): FResp[String] = {
    userRepo.findKeyBySecret(secret).map {
      _.fold[Resp[String]](
        Left(AuthenticationError)
      )(key => Right(jwt.encode(JwtServicePayload(key.value))))
    }
  }

  def authenticateWithTemporalKey(secret: String): FResp[String] = {
    val jwtStr = Option(cache.getIfPresent(secret))
    cache.invalidate(secret)
    Future.successful(jwtStr.toRight(AuthenticationError))
  }

  def insertNewApiKey(userExtId: String, name: String): FResp[ApiKey] = {
    userRepo
      .generateNewApiKeyToUser(userExtId, name)
      .map(
        _.fold[Resp[ApiKey]](
          Left(AuthenticationError)
        )(
          Right(_)
        )
      )
  }

  def importNewApiKey(userExtId: String, fromExt: String, name: String, secret: String): FResp[Completed] = {
    userRepo
      .importApiKey(fromExt, userExtId, name, secret)
      .map(
        _.fold[Resp[Completed]](
          Left(NoEntityFound)
        )(
          Right(_)
        )
      )
  }

  def listAllApiKeys(userExtId: String): FResp[Seq[ApiKey]] = {
    userRepo
      .findUserByExtId(userExtId)
      .map(
        _.fold[Resp[Seq[ApiKey]]](Left(AuthenticationError))(user => Right(user.keys))
      )
  }

  def authenticateWithGoogle(code: String): FResp[String] = {
    (for {
      ac <- getAccessToken(code).map(_.access_token)
      userInfo <- getUserInfo(ac)
    } yield userInfo) flatMap { userInfo =>
      {
        userRepo.findUserByExtId(userInfo.sub).flatMap { dbUser =>
          {
            if (dbUser.isEmpty) {
              userRepo.insertNewUser(User(userInfo.sub, userInfo.email, Nil)).map(_ => addJwtToCache(userInfo.sub))
            } else {
              Future.successful(addJwtToCache(userInfo.sub))
            }
          }
        }
      }
    } recover {
      case ex =>
        ex.printStackTrace()
        Left(AuthenticationError)
    }
  }

  def addJwtToCache(extId: String): Resp[String] = {
    val jwtStr = jwt.encode(JwtPayload(extId))
    val uid = GeneralUtil.uuid
    cache.put(uid, jwtStr)
    Right(uid)
  }

  def getAuthUrl: Resp[String] = {
    val url = s"""$authorizationEndpoint?
                 |client_id=${config.GOOGLE.clientId}&
                 |redirect_uri=${config.GOOGLE.callback}&
                 |response_type=code&
                 |scope=${config.GOOGLE.scope}&
                 |access_type=online
                 |""".stripMargin.replaceAll("[\r\n]+", "")
    Right(url)
  }

  private def getAccessToken(code: String) = {
    val requestObject = Map[String, String](
      "code" -> code,
      "client_id" -> config.GOOGLE.clientId,
      "client_secret" -> config.GOOGLE.clientSecret,
      "redirect_uri" -> config.GOOGLE.callback,
      "grant_type" -> "authorization_code"
    )
    val entity = FormData(requestObject).toEntity
    val request = HttpRequest(method = HttpMethods.POST, uri = Uri(tokenEndpoint), entity = entity)
    for {
      response <- Http().singleRequest(request)
      _ = if(response.status == StatusCodes.Unauthorized) println("request to google get unauth") else println(s"${response.status} status after google auth")
      body <- Unmarshal(response.entity).to[AuthResponse]
    } yield body
  }

  private def getUserInfo(access_token: String): Future[UserInfo] = {
    val authHeader = HttpHeader.parse("Authorization", s"Bearer $access_token") match {
      case HttpHeader.ParsingResult.Ok(h, Nil) => h
      case _ => throw new Exception("Invalid header")
    }
    val request = HttpRequest(uri = Uri(userInfoEndpoint)).withHeaders(authHeader)
    for {
      response <- Http().singleRequest(request)
      body <- Unmarshal(response.entity).to[UserInfo]
    } yield body
  }
}

object AuthService {

  trait AuthServiceJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

    import org.json4s.DefaultFormats

    implicit val jsonFormats = DefaultFormats

    implicit val userInfoJsonFormatter: RootJsonFormat[UserInfo] = jsonFormat9(UserInfo)
    implicit val authResponseJsonFormatter: RootJsonFormat[AuthResponse] = jsonFormat4(AuthResponse)
  }

  case class UserInfo(
      sub: String,
      name: String,
      given_name: String,
      family_name: String,
      profile: String,
      picture: String,
      email: String,
      email_verified: Boolean,
      gender: String
  )

  case class AuthResponse(
      access_token: String,
      token_type: String,
      expires_in: Int,
      id_token: String
  )

}
