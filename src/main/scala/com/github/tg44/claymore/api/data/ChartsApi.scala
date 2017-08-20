package com.github.tg44.claymore.api.data

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.tg44.claymore.jwt.Jwt
import com.github.tg44.claymore.service.ChartService
import com.github.tg44.claymore.utils.GeneralUtil
import scaldi.{Injectable, Injector}

class ChartsApi(implicit injector: Injector) extends ChartsJsonSupport with Injectable {

  private val jwt = inject[Jwt]
  import jwt._
  private val chartService = inject[ChartService]

  val route: Route = {
    (path("chart") & get) {
      authenticatedWithData { jwtData =>
        onSuccess(chartService.getCharts(jwtData.userId, GeneralUtil.nowInUnix - (14 * 24 * 60 * 60), GeneralUtil.nowInUnix)) {
          case Right(charts) =>
            complete(charts)
          case Left(_) => complete(HttpResponse(BadRequest))
        }
      }
    }
  }
}
