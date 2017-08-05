package com.github.tg44.claymore.api.google

import akka.http.scaladsl.model.StatusCodes.Unauthorized
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.tg44.claymore.jwt.Jwt
import com.github.tg44.claymore.service.AuthService
import scaldi.{Injectable, Injector}

class GoogleAuthApi(implicit injector: Injector) extends Injectable {

  private val jwt = inject[Jwt]
  private val authService = inject[AuthService]

  val route: Route = {
    handleOAuth2Callback ~
    googleAuthStart
  }

  private val handleOAuth2Callback = (path("oauth2callback") & get) {
    (parameter('code) & parameter('state)) {
      case (code, state) =>
        onSuccess(authService.authenticateWithGoogle(code, state)) {
          case Right(jwt) =>
            complete(
              HttpResponse(status = StatusCodes.TemporaryRedirect, headers = headers.Location("/") :: RawHeader("Authorization", s"Bearer $jwt") :: Nil)
            )
          case Left(_) => complete(HttpResponse(Unauthorized))
        }
    }
  }

  private val googleAuthStart = (path("googleauth") & get) {
    authService.getAuthUrl match {
      case Right(redirectUri) => redirect(redirectUri, StatusCodes.TemporaryRedirect)
      case Left(_) => complete(HttpResponse(StatusCodes.InternalServerError))
    }
  }

}
