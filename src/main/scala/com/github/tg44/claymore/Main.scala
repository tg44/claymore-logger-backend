package com.github.tg44.claymore

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import com.github.tg44.claymore.api.service.ServiceApi
import com.github.tg44.claymore.repository.{MongoDb, MongoDbImpl}
import com.github.tg44.claymore.repository.measures.MeasureRepo
import com.github.tg44.claymore.repository.users.UserRepo
import com.github.tg44.claymore.service.{AuthService, StatisticDataService}
import scaldi.{Injectable, Module}

import scala.concurrent.Future

object Main extends App with Injectable {
  import AkkaImplicits._

  object MainModule extends Module {
    binding toProvider new MongoDbImpl
    binding toProvider new UserRepo
    binding toProvider new MeasureRepo

    binding toProvider new ServiceApi

    binding toProvider new AuthService
    binding toProvider new StatisticDataService
  }

  Config.SERVER
  startApplication

  def startApplication(): Unit = {
    implicit val modules = MainModule
    import Config.SERVER

    val serviceApi = inject[ServiceApi]

    val routes = pathPrefix("api") {
      serviceApi.route
    }

    val adminApiBindingFuture: Future[ServerBinding] = Http()
      .bindAndHandle(routes, SERVER.url, SERVER.port)
      .map(binding => {
        println(s"Server started on ${SERVER.url}:${SERVER.port}")
        binding
      })
  }

}
