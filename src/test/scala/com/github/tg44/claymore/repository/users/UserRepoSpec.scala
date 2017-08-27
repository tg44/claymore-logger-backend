package com.github.tg44.claymore.repository.users

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.simplyscala.MongoEmbedDatabase
import com.github.tg44.claymore.utils.AppFixture.withMongoDb
import com.github.tg44.claymore.utils.GeneralUtil
import org.mongodb.scala.{Completed, SingleObservable}
import org.scalatest.{Matchers, WordSpecLike}
import scaldi.Injectable

import scala.concurrent.Await

class UserRepoSpec extends WordSpecLike with Matchers with Injectable {

  import com.github.tg44.claymore.utils.AppFixture._
  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  "UserRepo" must {

    val testKey1 = ApiKey("testkey1", "key1", "testId1", 0)
    val testKey2 = ApiKey("testkey2", "key2", "testId1", 0)
    val user1 = User("testId1", "test1@email.com", Nil)
    val user1Duplicated = User("testId1", "test1@email.com", Nil)
    val user2 = User("testId2", "test2@email.com", Nil)
    val user1With1Key = User("testId1", "test1@email.com", Seq(testKey1))
    val user1With2Key = User("testId1", "test1@email.com", Seq(testKey1, testKey2))
    val user2With1Key = User("testId2", "test2@email.com", Seq(testKey2))
    val user2WithDuplicatedKey = User("testId2", "test2@email.com", Seq(testKey1))

    "insert new user" must {

      "add user" in withMongoDb() { module =>
        import module._
        val userRepo = inject[UserRepo]
        Await.result(userRepo.insertNewUser(user1), dbTimeout) shouldBe Completed()
        val user = Await.result(userRepo.findUserByExtId("testId1"), dbTimeout)
        user should matchPattern { case Some(User(_, "testId1", "test1@email.com", _)) => }
        Await.result(userRepo.collection.count().toFuture, dbTimeout) shouldBe 1
      }
    }

    "find user by extid" must {

      "if it exists" in withMongoDb() { module =>
        import module._
        val userRepo = inject[UserRepo]
        Await.result(userRepo.collection.insertOne(user1).toFuture, dbTimeout)
        Await.result(userRepo.collection.insertOne(user2).toFuture, dbTimeout)
        val result = Await.result(userRepo.findUserByExtId("testId1"), dbTimeout)
        result should matchPattern { case Some(User(_, "testId1", "test1@email.com", Nil)) => }
      }

      "if somehow multiple exists" in withMongoDb() { module =>
        import module._
        val userRepo = inject[UserRepo]
        Await.result(userRepo.collection.insertOne(user1).toFuture, dbTimeout)
        Await.result(userRepo.collection.insertOne(user1Duplicated).toFuture, dbTimeout)
        val result = Await.result(userRepo.findUserByExtId("testId1"), dbTimeout)
        result shouldBe None
      }

      "if none exist" in withMongoDb() { module =>
        import module._
        val userRepo = inject[UserRepo]
        val result = Await.result(userRepo.findUserByExtId("no"), dbTimeout)
        result shouldBe None
      }
    }

    "find key by secret" must {

      "if it exists" in withMongoDb() { module =>
        import module._
        val userRepo = inject[UserRepo]
        Await.result(userRepo.collection.insertOne(user1With1Key).toFuture, dbTimeout)
        Await.result(userRepo.collection.insertOne(user2With1Key).toFuture, dbTimeout)
        val result = Await.result(userRepo.findKeyBySecret("key1"), dbTimeout)
        result shouldBe Some(testKey1)
      }

      "if has multiple" in withMongoDb() { module =>
        import module._
        val userRepo = inject[UserRepo]
        Await.result(userRepo.collection.insertOne(user1With2Key).toFuture, dbTimeout)
        val result = Await.result(userRepo.findKeyBySecret("key2"), dbTimeout)
        result shouldBe Some(testKey2)
      }

      "if none exist" in withMongoDb() { module =>
        import module._
        val userRepo = inject[UserRepo]
        val result = Await.result(userRepo.findKeyBySecret("key1"), dbTimeout)
        result shouldBe None
      }
    }

    "generate new ApiKey" must {

      "if user exists" in withMongoDb() { module =>
        import module._
        val userRepo = inject[UserRepo]
        val time = GeneralUtil.nowInUnix
        Await.result(userRepo.collection.insertOne(user1).toFuture, dbTimeout)
        Await.result(userRepo.collection.insertOne(user2).toFuture, dbTimeout)
        val result = Await.result(userRepo.generateNewApiKeyToUser("testId1", "key1"), dbTimeout)
        result should matchPattern { case Some(ApiKey("key1", _, _, z)) if z >= time => }
        val user = Await.result(userRepo.findUserByExtId("testId1"), dbTimeout)
        user should matchPattern { case Some(User(_, "testId1", "test1@email.com", Seq(ApiKey("key1", _, _, _)))) => }
      }

      "if user not exist" in withMongoDb() { module =>
        import module._
        val userRepo = inject[UserRepo]
        val result = Await.result(userRepo.generateNewApiKeyToUser("no", "key1"), dbTimeout)
        result shouldBe None
      }

      "if user has other keys" in withMongoDb() { module =>
        import module._
        val userRepo = inject[UserRepo]
        val time = GeneralUtil.nowInUnix
        Await.result(userRepo.collection.insertOne(user1With2Key).toFuture, dbTimeout)
        val result = Await.result(userRepo.generateNewApiKeyToUser("testId1", "key1"), dbTimeout)
        result should matchPattern { case Some(ApiKey("key1", _, _, z)) if z >= time => }
        val user = Await.result(userRepo.findUserByExtId("testId1"), dbTimeout)
        user should matchPattern { case Some(User(_, "testId1", "test1@email.com", seq)) if seq.size == 3 => }
      }

    }

    "delete apiKey" must {

      "if user and key existed" in withMongoDb() { module =>
        import module._
        val userRepo = inject[UserRepo]
        Await.result(userRepo.collection.insertOne(user1With2Key).toFuture, dbTimeout)
        val result = Await.result(userRepo.deleteApiKey("testId1", "key2"), dbTimeout)
        result shouldBe Some(Completed())
        val user = Await.result(userRepo.findUserByExtId("testId1"), dbTimeout)
        user should matchPattern { case Some(User(_, "testId1", "test1@email.com", seq)) if seq.size == 1 => }
      }

      "if user existed but key not" in withMongoDb() { module =>
        import module._
        val userRepo = inject[UserRepo]
        Await.result(userRepo.collection.insertOne(user1With2Key).toFuture, dbTimeout)
        val result = Await.result(userRepo.deleteApiKey("testId1", "no"), dbTimeout)
        result shouldBe Some(Completed())
        val user = Await.result(userRepo.findUserByExtId("testId1"), dbTimeout)
        user should matchPattern { case Some(User(_, "testId1", "test1@email.com", seq)) if seq.size == 2 => }
      }

      "if user not but key existed in somewhere else" in withMongoDb() { module =>
        import module._
        val userRepo = inject[UserRepo]
        Await.result(userRepo.collection.insertOne(user1With2Key).toFuture, dbTimeout)
        val result = Await.result(userRepo.deleteApiKey("no", "key1"), dbTimeout)
        result shouldBe None
        val user = Await.result(userRepo.findUserByExtId("testId1"), dbTimeout)
        user should matchPattern { case Some(User(_, "testId1", "test1@email.com", seq)) if seq.size == 2 => }
      }
    }

  }
}
