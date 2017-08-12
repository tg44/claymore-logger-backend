package com.github.tg44.claymore.api.google

import akka.http.scaladsl.model.StatusCodes.Unauthorized
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.tg44.claymore.config.Config
import com.github.tg44.claymore.service.AuthService
import scaldi.{Injectable, Injector}

class GoogleAuthApi(implicit injector: Injector) extends Injectable with GoogleJsonSupport {

  private val authService = inject[AuthService]
  private val config = inject[Config]

  def route: Route = {
    handleOAuth2Callback ~
    googleAuthStart ~
    getJwt
  }

  private val handleOAuth2Callback = (path("oauth2callback") & get) {
    (parameter('code)) {
      case (code) =>
        onSuccess(authService.authenticateWithGoogle(code)) {
          case Right(uid) =>
            complete(
              HttpResponse(status = StatusCodes.TemporaryRedirect, headers = headers.Location(config.SPENDPOINTS.callbackAfterAuth + uid) :: Nil)
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

  private val getJwt = path("jwt") {
    (post & entity(as[ApiSecurityDto])) { securityDto =>
      onSuccess(authService.authenticateWithTemporalKey(securityDto.secret)) {
        case Right(jwt) => complete(ApiSecurityAnswerDto(jwt))
        case Left(_) => complete(HttpResponse(Unauthorized))
      }
    }
  }

}
