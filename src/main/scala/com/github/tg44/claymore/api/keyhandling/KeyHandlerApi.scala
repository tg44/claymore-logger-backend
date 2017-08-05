package com.github.tg44.claymore.api.keyhandling

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives.{as, complete, entity, onSuccess, path, post, _}
import akka.http.scaladsl.server.Route
import com.github.tg44.claymore.jwt.Jwt
import com.github.tg44.claymore.service.AuthService
import scaldi.{Injectable, Injector}

class KeyHandlerApi(implicit injector: Injector) extends Injectable with KeysJsonSupport {

  private val jwt = inject[Jwt]
  import jwt._
  private val authService = inject[AuthService]

  val route: Route = {
    path("insert") {
      authenticatedWithData { jwtData =>
        (post & entity(as[Name])) { nameDao =>
          onSuccess(authService.insertNewApiKey(jwtData.userId, nameDao.name)) {
            case Right(response) => complete(response)
            case Left(_) => complete(HttpResponse(BadRequest))
          }
        }
      }
    } ~
    path("load") {
      authenticatedWithData { jwtData =>
        get {
          onSuccess(authService.listAllApiKeys(jwtData.userId)) {
            case Right(response) => complete(ApiKeyList(response))
            case Left(_) => complete(HttpResponse(BadRequest))
          }
        }
      }
    }

  }
}
