package com.github.tg44.claymore.api.service

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.{as, complete, entity, onSuccess, path, post, _}
import akka.http.scaladsl.model.StatusCodes._
import com.github.tg44.claymore.jwt.Jwt
import com.github.tg44.claymore.service.{AuthService, StatisticDataService}
import scaldi.{Injectable, Injector}

class ServiceApi(implicit injector: Injector) extends ServiceJsonSupport with Injectable {

  private val jwt = inject[Jwt]
  import jwt._
  private val authService = inject[AuthService]
  private val statisticDataService = inject[StatisticDataService]

  val route: Route = {
    path("jwt") {
      (post & entity(as[ApiSecurityDto])) { securityDto =>
        onSuccess(authService.authenticateWithApiKey(securityDto.secret)) {
          case Right(jwt) => complete(jwt)
          case Left(_) => complete(HttpResponse(Unauthorized))
        }
      }
    } ~
    path("data") {
      authenticatedWithData { jwtData =>
        (post & entity(as[StatisticDataDto])) { statDataDto =>
          onSuccess(statisticDataService.saveData(jwtData.userId, statDataDto)) {
            case Right(response) => complete(response.toString)
            case Left(_) => complete(HttpResponse(BadRequest))
          }
        }
      }
    }
  }
}
