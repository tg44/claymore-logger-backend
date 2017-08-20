package com.github.tg44.claymore.api.data

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.tg44.claymore.jwt.Jwt
import com.github.tg44.claymore.service.StatisticDataService
import com.github.tg44.claymore.utils.GeneralUtil
import scaldi.{Injectable, Injector}

class StatisticalDataApi(implicit injector: Injector) extends StatDataJsonSupport with Injectable {

  private val jwt = inject[Jwt]
  import jwt._
  private val statisticDataService = inject[StatisticDataService]

  val route: Route = {
    (path("stats") & get) {
      authenticatedWithData { jwtData =>
        onSuccess(statisticDataService.getLastMeasures(jwtData.userId, GeneralUtil.nowInUnix - (14 * 24 * 60 * 60), GeneralUtil.nowInUnix)) {
          case Right(data) =>
            complete(StatisticDataresponseDto(data))
          case Left(_) => complete(HttpResponse(BadRequest))
        }
      }
    }
  }
}
