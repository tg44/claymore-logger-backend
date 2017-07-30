package com.github.tg44.claymore.repository.measures

import java.security.InvalidParameterException

import com.github.tg44.claymore.repository.users.UserRepo
import org.scalatest.{Matchers, WordSpecLike}
import scaldi.Injectable

import scala.concurrent.Await

class MeasureRepoSpec extends WordSpecLike with Matchers with Injectable {

  import com.github.tg44.claymore.utils.AppFixture._

  "MeasureRepo" must {

    def insertTestDataset(measureRepo: MeasureRepo) = {
      Await.result(
        measureRepo.collection
          .insertMany(
            Seq(
              Measure("testExtId", 101, 200, Nil),
              Measure("testExtId", 201, 300, Nil),
              Measure("testExtId", 401, 500, Nil),
              Measure("testExtId2", 101, 200, Nil)
            )
          )
          .toFuture,
        dbTimeout
      )
    }

    def createStatisticData(time: Long): StatisticData = {
      val ci = CurrencyInformation(0.0, 1, 1, 1, 1, Nil, "")
      StatisticData(time, "", "", 1, "", 1.0, ci, ci, Nil, Nil, Nil)
    }

    "getMesuresInRange" must {

      "throw exception to bad input parameters" in withMongoDb { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        an[IllegalArgumentException] should be thrownBy Await.result(measureRepo.getMesuresInRange("testExtId", 500, 100), dbTimeout)
      }

      "return nothing if no matching document" in withMongoDb { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        val result = Await.result(measureRepo.getMesuresInRange("testExtId", 305, 370), dbTimeout)
        result.size shouldBe 0
      }

      "return one if both from and to in a range" in withMongoDb { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        val result = Await.result(measureRepo.getMesuresInRange("testExtId", 420, 470), dbTimeout)
        result.size shouldBe 1
      }

      "return one if only the to in a range" in withMongoDb { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        val result = Await.result(measureRepo.getMesuresInRange("testExtId", 320, 470), dbTimeout)
        result.size shouldBe 1
      }

      "return one if onli the from in a range" in withMongoDb { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        val result = Await.result(measureRepo.getMesuresInRange("testExtId", 220, 320), dbTimeout)
        result.size shouldBe 1
      }

      "return multiple documents" in withMongoDb { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        val result = Await.result(measureRepo.getMesuresInRange("testExtId", 150, 450), dbTimeout)
        result.size shouldBe 3
      }

      "give back document only for the requested user" in withMongoDb { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        val result = Await.result(measureRepo.getMesuresInRange("testExtId", 150, 170), dbTimeout)
        result.size shouldBe 1
      }

    }

    "saveNewMeasure" must {

      "throw exception to bad period parameters" in withMongoDb { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        an[IllegalArgumentException] should be thrownBy Await.result(measureRepo.saveNewMeasure("testExtId", createStatisticData(100), (500, 100)), dbTimeout)
      }

      "throw exception to bad data timestamp parameters" in withMongoDb { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        an[IllegalArgumentException] should be thrownBy Await.result(measureRepo.saveNewMeasure("testExtId", createStatisticData(100), (300, 400)), dbTimeout)
      }

      "throw exception to bad number of documents given back from period" in withMongoDb { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        an[InvalidParameterException] should be thrownBy Await.result(measureRepo.saveNewMeasure("testExtId", createStatisticData(100), (100, 500)), dbTimeout)
      }

      "create new document if needed" in withMongoDb { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        Await.result(measureRepo.saveNewMeasure("testExtId", createStatisticData(351), (350, 370)), dbTimeout)
        Await.result(measureRepo.collection.count().toFuture, dbTimeout) shouldBe 5
      }

      "append existing document if has any" in withMongoDb { module =>
        import module._
        val measureRepo = inject[MeasureRepo]
        insertTestDataset(measureRepo)
        Await.result(measureRepo.saveNewMeasure("testExtId", createStatisticData(130), (120, 140)), dbTimeout)
        Await.result(measureRepo.collection.count().toFuture, dbTimeout) shouldBe 4
        val result = Await.result(measureRepo.getMesuresInRange("testExtId", 120, 140), dbTimeout)
        result.size shouldBe 1
        result.head.data.size shouldBe 1
      }

    }

  }

}
