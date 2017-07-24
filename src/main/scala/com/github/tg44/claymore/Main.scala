package com.github.tg44.claymore

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import com.github.tg44.claymore.api.service.ServiceApi

import scala.concurrent.Future

object Main extends App {

  import AkkaImplicits._

  Config.SERVER
  startApplication

  def startApplication(): Unit = {
    import Config.SERVER

    //implicit val modules = Modules :: SystemImplicits
    val serviceApi = new ServiceApi()

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
