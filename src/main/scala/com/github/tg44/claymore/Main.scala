package com.github.tg44.claymore

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import com.github.tg44.claymore.api.google.GoogleAuthApi
import com.github.tg44.claymore.api.keyhandling.KeyHandlerApi
import com.github.tg44.claymore.api.service.ServiceApi
import com.github.tg44.claymore.config.{Config, ConfigImpl}
import com.github.tg44.claymore.jwt.Jwt
import com.github.tg44.claymore.repository.{MongoDb, MongoDbImpl}
import com.github.tg44.claymore.repository.measures.MeasureRepo
import com.github.tg44.claymore.repository.users.UserRepo
import com.github.tg44.claymore.service.{AuthService, StatisticDataService}
import scaldi.{Injectable, Module}

import scala.concurrent.Future

object Main extends App with Injectable {
  import AkkaImplicits._

  object MainModule extends Module {
    binding toProvider new ConfigImpl
    binding toProvider new Jwt

    binding toProvider new MongoDbImpl
    binding toProvider new UserRepo
    binding toProvider new MeasureRepo

    binding toProvider new ServiceApi
    binding toProvider new GoogleAuthApi
    binding toProvider new KeyHandlerApi

    binding toProvider new AuthService
    binding toProvider new StatisticDataService
  }

  startApplication

  def startApplication(): Unit = {
    implicit val modules = MainModule

    val serviceApi = inject[ServiceApi]
    val googleAuthApi = inject[GoogleAuthApi]
    val keyHandlerApi = inject[KeyHandlerApi]
    val config = inject[Config]

    val routes = pathPrefix("service") {
      serviceApi.route
    } ~ pathPrefix("google") {
      googleAuthApi.route
    } ~ pathPrefix("api") {
      pathPrefix("keys") {
        keyHandlerApi.route
      }
    }

    val adminApiBindingFuture: Future[ServerBinding] = Http()
      .bindAndHandle(routes, config.SERVER.url, config.SERVER.port)
      .map(binding => {
        println(s"Server started on ${config.SERVER.url}:${config.SERVER.port}")
        binding
      })
  }

}
