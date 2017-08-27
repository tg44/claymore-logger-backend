package com.github.tg44.claymore.repository.measures

import java.security.InvalidParameterException

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{Matchers, WordSpecLike}
import scaldi.Injectable

import scala.concurrent.Await

class MeasureRepoSpec extends WordSpecLike with Matchers with Injectable {

  import com.github.tg44.claymore.utils.AppFixture._
  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  "MeasureRepo" must {

    def insertTestDataset(measureRepo: MeasureRepo) = {
      Await.result(
        measureRepo.collection
          .insertMany(
            Seq(
              Measure("testKey", 101, 200, Nil),
              Measure("testKey", 201, 300, Nil),
              Measure("testKey", 401, 500, Nil),
              Measure("testKey2", 101, 200, Nil)
            )
          )
          .toFuture,
        dbTimeout
      )
    }

    def createStatisticData(time: Long): StatisticData = {
      val ci = CurrencyInformation("eth", 0.0, 1, 1, 1, 1, Nil, "")
      StatisticData(time, "", "", 1, "", 1.0, Seq(ci), Nil, Nil, Nil)
    }

    "getMesuresInRange" must {

      "throw exception to bad input parameters" in withMongoDb() { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        an[IllegalArgumentException] should be thrownBy Await.result(measureRepo.getMesuresInRange(Seq("testKey"), 500, 100), dbTimeout)
      }

      "return nothing if no matching document" in withMongoDb() { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        val result = Await.result(measureRepo.getMesuresInRange(Seq("testKey"), 305, 370), dbTimeout)
        result.size shouldBe 0
      }

      "return one if both from and to in a range" in withMongoDb() { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        val result = Await.result(measureRepo.getMesuresInRange(Seq("testKey"), 420, 470), dbTimeout)
        result.size shouldBe 1
      }

      "return one if only the to in a range" in withMongoDb() { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        val result = Await.result(measureRepo.getMesuresInRange(Seq("testKey"), 320, 470), dbTimeout)
        result.size shouldBe 1
      }

      "return one if onli the from in a range" in withMongoDb() { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        val result = Await.result(measureRepo.getMesuresInRange(Seq("testKey"), 220, 320), dbTimeout)
        result.size shouldBe 1
      }

      "return multiple documents" in withMongoDb() { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        val result = Await.result(measureRepo.getMesuresInRange(Seq("testKey"), 150, 450), dbTimeout)
        result.size shouldBe 3
      }

      "give back document only for the requested user" in withMongoDb() { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        val result = Await.result(measureRepo.getMesuresInRange(Seq("testKey"), 150, 170), dbTimeout)
        result.size shouldBe 1
      }

      "give back all document for the requested user" in withMongoDb() { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        val result = Await.result(measureRepo.getMesuresInRange(Seq("testKey", "testKey2"), 150, 170), dbTimeout)
        result.size shouldBe 2
      }
    }

    "saveNewMeasure" must {

      "throw exception to bad period parameters" in withMongoDb() { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        an[IllegalArgumentException] should be thrownBy Await.result(measureRepo.saveNewMeasure("testKey", createStatisticData(100), (500, 100)), dbTimeout)
      }

      "throw exception to bad data timestamp parameters" in withMongoDb() { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        an[IllegalArgumentException] should be thrownBy Await.result(measureRepo.saveNewMeasure("testKey", createStatisticData(100), (300, 400)), dbTimeout)
      }

      "throw exception to bad number of documents given back from period" in withMongoDb() { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        an[InvalidParameterException] should be thrownBy Await.result(measureRepo.saveNewMeasure("testKey", createStatisticData(100), (100, 500)), dbTimeout)
      }

      "create new document if needed" in withMongoDb() { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        Await.result(measureRepo.saveNewMeasure("testKey", createStatisticData(351), (350, 370)), dbTimeout)
        Await.result(measureRepo.collection.count().toFuture, dbTimeout) shouldBe 5
      }

      "create new document for new user" in withMongoDb() { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        Await.result(measureRepo.saveNewMeasure("testKey100", createStatisticData(351), (350, 370)), dbTimeout)
        Await.result(measureRepo.collection.count().toFuture, dbTimeout) shouldBe 5
      }

      "append existing document if has any" in withMongoDb() { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        Await.result(measureRepo.saveNewMeasure("testKey", createStatisticData(130), (120, 140)), dbTimeout)
        Await.result(measureRepo.collection.count().toFuture, dbTimeout) shouldBe 4
        val result = Await.result(measureRepo.getMesuresInRange(Seq("testKey"), 120, 140), dbTimeout)
        result.size shouldBe 1
        result.head.data.size shouldBe 1
      }

    }

  }

}
