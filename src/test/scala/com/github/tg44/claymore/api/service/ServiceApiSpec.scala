package com.github.tg44.claymore.api.service

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.tg44.claymore.jwt.{Jwt, JwtPayload}
import com.github.tg44.claymore.repository.users.{ApiKey, User, UserRepo}
import org.scalatest.{Matchers, WordSpec}
import scaldi.Injectable
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import com.github.tg44.claymore.config.{JwtProperties, Server}
import com.github.tg44.claymore.repository.measures.{CurrencyInformation, MeasureRepo, StatisticData}

import scala.concurrent.Await

class ServiceApiSpec extends WordSpec with Matchers with ScalatestRouteTest with Injectable with ServiceJsonSupport {

  import com.github.tg44.claymore.utils.AppFixture._

  "ServiceApi" must {

    "jwt endpoint" must {

      "give back unauth if bad apiKey given" in withMongoDb() { module =>
        import module._
        val serviceApi = inject[ServiceApi]

        val entity = HttpEntity(ContentTypes.`application/json`, """{"secret": "secret"}""")

        Post("/jwt", entity) ~> serviceApi.route ~> check {
          status shouldBe Unauthorized
        }
      }

      "give back ok if good apiKey given" in withMongoDb() { module =>
        import module._
        val serviceApi = inject[ServiceApi]
        val userRepo = inject[UserRepo]
        val jwt = inject[Jwt]

        Await.result(userRepo.insertNewUser(User("1", "asd@asd.asd", Seq(ApiKey("test", "secret", 0)))), dbTimeout)
        val entity = HttpEntity(ContentTypes.`application/json`, """{"secret": "secret"}""")

        Post("/jwt", entity) ~> serviceApi.route ~> check {
          status shouldBe OK
          jwt.decode[JwtPayload](responseAs[String]) shouldBe a[scala.util.Success[_]]
        }
      }

    }

    "data endpoint" must {

      val serverConfWithoutProtection = Server("localhost", 3000, needAuth = false, JwtProperties("test", 50000))

      val ci = CurrencyInformation(0.0, 1, 1, 1, 1, Nil, "")
      val statisticData = StatisticDataDto("", "", 1, ParsedStatisticResponse("", 1.0, ci, ci, Nil, Nil, Nil))
      import spray.json._

      "save data with no jwt protection" in withMongoDb(serverConfig = serverConfWithoutProtection) { module =>
        import module._
        val serviceApi = inject[ServiceApi]
        val measureRepo = inject[MeasureRepo]

        val entity = HttpEntity(ContentTypes.`application/json`, statisticData.toJson.compactPrint)

        Post("/data", entity) ~> serviceApi.route ~> check {
          status shouldBe OK
          responseAs[String] shouldBe "5000"
          Await.result(measureRepo.collection.count().toFuture, dbTimeout) shouldBe 1
        }
      }

      "save data with jwt protection and good header" in withMongoDb() { module =>
        import module._
        val serviceApi = inject[ServiceApi]
        val measureRepo = inject[MeasureRepo]
        val jwt = inject[Jwt]

        val token = jwt.encode(JwtPayload("0"))
        val authHeader = RawHeader("Authorization", s"Bearer $token")
        val entity = HttpEntity(ContentTypes.`application/json`, statisticData.toJson.compactPrint)

        Post("/data", entity) ~> addHeader(authHeader) ~> serviceApi.route ~> check {
          status shouldBe OK
          responseAs[String] shouldBe "5000"
          Await.result(measureRepo.collection.count().toFuture, dbTimeout) shouldBe 1
        }
      }

      "not data with jwt protection and bad header" in withMongoDb() { module =>
        import module._
        val serviceApi = inject[ServiceApi]
        val measureRepo = inject[MeasureRepo]
        val jwt = inject[Jwt]

        val token = "asdasdasd"
        val authHeader = RawHeader("Authorization", s"Bearer $token")
        val entity = HttpEntity(ContentTypes.`application/json`, statisticData.toJson.compactPrint)

        Post("/data", entity) ~> addHeader(authHeader) ~> serviceApi.route ~> check {
          rejection shouldBe AuthorizationFailedRejection
        }
      }

      "not data with jwt protection and missing header" in withMongoDb() { module =>
        import module._
        val serviceApi = inject[ServiceApi]
        val measureRepo = inject[MeasureRepo]

        val entity = HttpEntity(ContentTypes.`application/json`, statisticData.toJson.compactPrint)

        Post("/data", entity) ~> serviceApi.route ~> check {
          rejection shouldBe MissingHeaderRejection("Authorization")
        }

      }

    }

  }

}
