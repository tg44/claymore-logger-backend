package com.github.tg44.claymore.api.keyhandling

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.MissingHeaderRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.tg44.claymore.jwt.{Jwt, JwtPayload}
import com.github.tg44.claymore.repository.users.{ApiKey, User, UserRepo}
import org.scalatest.{Matchers, WordSpec}
import scaldi.Injectable

import scala.concurrent.Await

class KeyHandlerApiSpec extends WordSpec with Matchers with ScalatestRouteTest with Injectable with KeysJsonSupport {
  import com.github.tg44.claymore.utils.AppFixture._

  "KeyHandlerApi" must {

    val user1 = User("testId1", "test1@email.com", Nil)
    val user2 = User("testId2", "test2@email.com", Seq(ApiKey("1", "1", 1), ApiKey("2", "2", 2), ApiKey("3", "3", 3)))

    "insert endpoint" must {

      "need authentication" in withMongoDb() { module =>
        import module._
        val keyApi = inject[KeyHandlerApi]

        val entity = HttpEntity(ContentTypes.`application/json`, """{"name": "test"}""")

        Post("/insert", entity) ~> keyApi.route ~> check {
          rejection shouldBe MissingHeaderRejection("Authorization")
        }
      }

      "return 400 if no user saved" in withMongoDb() { module =>
        import module._
        val keyApi = inject[KeyHandlerApi]
        val jwt = inject[Jwt]

        val token = jwt.encode(JwtPayload("0"))
        val authHeader = RawHeader("Authorization", s"Bearer $token")
        val entity = HttpEntity(ContentTypes.`application/json`, """{"name": "test"}""")

        Post("/insert", entity) ~> addHeader(authHeader) ~> keyApi.route ~> check {
          status shouldBe BadRequest
        }
      }

      "insert new key correctly" in withMongoDb() { module =>
        import module._
        val keyApi = inject[KeyHandlerApi]
        val jwt = inject[Jwt]
        val userRepo = inject[UserRepo]

        val token = jwt.encode(JwtPayload(user1.extid))
        val authHeader = RawHeader("Authorization", s"Bearer $token")
        val entity = HttpEntity(ContentTypes.`application/json`, """{"name": "test"}""")

        Await.result(userRepo.collection.insertOne(user1).toFuture, dbTimeout)

        Post("/insert", entity) ~> addHeader(authHeader) ~> keyApi.route ~> check {
          status shouldBe OK
          responseAs[ApiKey] should matchPattern { case ApiKey("test", _, _) => }
          val user = Await.result(userRepo.findUserByExtId(user1.extid), dbTimeout)
          user should matchPattern { case Some(User(_, "testId1", "test1@email.com", Seq(ApiKey("test", _, _)))) => }
        }
      }
    }
    "load endpoint" must {
      "need authentication" in withMongoDb() { module =>
        import module._
        val keyApi = inject[KeyHandlerApi]

        Get("/load") ~> keyApi.route ~> check {
          rejection shouldBe MissingHeaderRejection("Authorization")
        }
      }

      "return data correctly" in withMongoDb() { module =>
        import module._
        val keyApi = inject[KeyHandlerApi]
        val jwt = inject[Jwt]
        val userRepo = inject[UserRepo]

        val token = jwt.encode(JwtPayload(user2.extid))
        val authHeader = RawHeader("Authorization", s"Bearer $token")
        val entity = HttpEntity(ContentTypes.`application/json`, """{"name": "test"}""")

        Await.result(userRepo.collection.insertOne(user2).toFuture, dbTimeout)

        Get("/load") ~> addHeader(authHeader) ~> keyApi.route ~> check {
          status shouldBe OK
          responseAs[ApiKeyList].list shouldBe user2.keys
        }
      }
    }

  }
}
