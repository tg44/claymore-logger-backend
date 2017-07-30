package com.github.tg44.claymore.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.simplyscala.MongoEmbedDatabase

import scala.concurrent.ExecutionContext
import scala.util.Random
import scala.concurrent.duration._

object AppFixture extends MongoEmbedDatabase {

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val dbTimeout = 3 seconds

  var portCounter = 20000

  def withMongoDb(block: TestModule => Unit): Unit = {
    //TODO: this is not so safe...
    portCounter += 1
    val port = portCounter
    val mongodProps = mongoStart(port)
    implicit val modules = new TestModule(port)
    try { block(modules) } finally { Option(mongodProps).foreach(mongoStop) }
  }

}
