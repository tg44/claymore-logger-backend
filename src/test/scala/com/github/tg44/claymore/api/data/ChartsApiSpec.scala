package com.github.tg44.claymore.api.data

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.MissingHeaderRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.tg44.claymore.jwt.{Jwt, JwtPayload}
import com.github.tg44.claymore.repository.users.{User, UserRepo}
import com.github.tg44.claymore.service.{ChartData, Charts, MultilineChart, SingleLineChart}
import org.scalatest.{Matchers, WordSpec}
import scaldi.Injectable
import akka.http.scaladsl.model.StatusCodes._

import scala.concurrent.Await

class ChartsApiSpec extends WordSpec with Matchers with ScalatestRouteTest with Injectable with ChartsJsonSupport {

  import com.github.tg44.claymore.utils.AppFixture._

  "ChartsApi" must {
    val user1 = User("testId1", "test1@email.com", Nil)

    "chart endpoint" must {
      "need authentication" in withMongoDb() { module =>
        import module._
        val chartApi = inject[ChartsApi]

        Get("/chart") ~> chartApi.route ~> check {
          rejection shouldBe MissingHeaderRejection("Authorization")
        }
      }

      "return data correctly" in withMongoDb() { module =>
        import module._
        val chartApi = inject[ChartsApi]
        val jwt = inject[Jwt]
        val userRepo = inject[UserRepo]

        val token = jwt.encode(JwtPayload(user1.extid))
        val authHeader = RawHeader("Authorization", s"Bearer $token")

        Await.result(userRepo.collection.insertOne(user1).toFuture, dbTimeout)

        Get("/chart") ~> addHeader(authHeader) ~> chartApi.route ~> check {
          status shouldBe OK
          responseAs[Charts] shouldBe a[Charts]
        }
      }
    }
  }
}
