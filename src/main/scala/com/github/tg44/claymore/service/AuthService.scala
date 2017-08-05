package com.github.tg44.claymore.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.github.tg44.claymore.config.Config
import com.github.tg44.claymore.jwt.{Jwt, JwtPayload}
import com.github.tg44.claymore.repository.users.{ApiKey, User, UserRepo}
import com.github.tg44.claymore.service.AuthService.{AuthResponse, AuthServiceJsonSupport, UserInfo}
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

  def authenticateWithApiKey(secret: String): FResp[String] = {
    userRepo.findUserByApiKey(secret).map { usr =>
      usr.fold[Resp[String]](
        Left(AuthenticationError)
      )(user => Right(jwt.encode(JwtPayload(user.extid))))
    }
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

  def listAllApiKeys(userExtId: String): FResp[Seq[ApiKey]] = {
    userRepo
      .findUserByExtId(userExtId)
      .map(
        _.fold[Resp[Seq[ApiKey]]](Left(AuthenticationError))(user => Right(user.keys))
      )
  }

  def authenticateWithGoogle(code: String, state: String): FResp[String] = {
    (for {
      ac <- getAccessToken(code).map(_.access_token)
      userInfo <- getUserInfo(ac)
    } yield userInfo) flatMap { userInfo =>
      userRepo.findUserByExtId(userInfo.id).flatMap { dbUser =>
        if (dbUser.isEmpty) {
          userRepo.insertNewUser(User(userInfo.id, userInfo.email, Nil)).map(_ => Right(jwt.encode(JwtPayload(userInfo.id))))
        } else {
          Future.successful(Right(jwt.encode(JwtPayload(userInfo.id))))
        }
      }
    } recover {
      case _ => Left(AuthenticationError)
    }
  }

  def getAuthUrl: Resp[String] = {
    Right(
      s"""$authorizationEndpoint?
         |client_id=${config.GOOGLE.clientId}&
         |redirect_uri=${config.GOOGLE.callback}&
         |response_type=code&
         |scope=${config.GOOGLE.scope}&
         |access_type=online
         |""".stripMargin
    )
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
      id: String,
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
