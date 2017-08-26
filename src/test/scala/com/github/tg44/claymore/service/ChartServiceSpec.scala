package com.github.tg44.claymore.service

import akka.actor.ActorSystem
import com.github.tg44.claymore.repository.measures._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import scaldi.Module

class ChartServiceSpec extends WordSpecLike with Matchers with MockitoSugar {
  implicit val system: ActorSystem = ActorSystem("test")
  implicit val ec = system.dispatcher

  "ChartService" must {
    implicit val testModule = new Module {
      bind[MeasureRepo] to mock[MeasureRepo]
    }
    val service = new ChartService()

    val ci = CurrencyInformation("eth", 0.5, 3, 2, 1, 0, Nil, "")

    val measures = Seq(
      Measure(
        "a",
        1,
        10,
        Seq(
          StatisticData(1, "a", "", 1, "", 2880.0, Seq(ci), Seq(2, 8), Nil, Nil),
          StatisticData(2, "b", "", 1, "", 2880.0, Seq(ci), Seq(1), Seq(50), Seq(CardStatistic(Map(), 1, 50))),
          StatisticData(2, "c", "", 1, "", 2880.0, Seq(ci), Nil, Nil, Nil),
          StatisticData(6, "a", "", 1, "", 2880.0, Seq(ci), Seq(6, 10), Nil, Nil)
        )
      ),
      Measure(
        "b",
        11,
        20,
        Seq(
          StatisticData(11, "a", "", 1, "", 2880.0, Seq(ci), Seq(2, 8), Nil, Nil),
          StatisticData(12, "b", "", 1, "", 2880.0, Seq(ci), Seq(1, 3), Seq(60, 70), Seq(CardStatistic(Map(), 1, 60), CardStatistic(Map(), 3, 70))),
          StatisticData(12, "d", "", 1, "", 4320.0, Seq(ci), Nil, Nil, Nil),
          StatisticData(16, "a", "", 1, "", 2880.0, Seq(ci), Seq(6, 10), Nil, Nil)
        )
      )
    )

    "aggregate measures by host correctly" in {
      val aggregated = ChartService.aggregateMeasuresByHost(measures)
      aggregated.size shouldBe 4
      aggregated("a").size shouldBe 4
      aggregated("b").size shouldBe 2
      aggregated("c").size shouldBe 1
      aggregated("d").size shouldBe 1
    }

    "aggregate measures by currencies and time correctly" in {
      val aggregated = ChartService.aggregateCurrenciesByTime(measures, 2)
      aggregated.size shouldBe 1
      aggregated("eth").size shouldBe 6
      aggregated("eth")(0).size shouldBe 1
      aggregated("eth")(2).size shouldBe 2
    }

    "compute avgTemp per host correctly" in {
      val aggregated = ChartService.aggregateMeasuresByHost(measures)
      val chart = service.computeAvgTempPerHost(aggregated)

      chart.title shouldBe "avgTemp"
      chart.legend should contain allOf ("a", "b", "c", "d")
      val bData = chart.data(chart.legend.indexOf("b"))
      bData.size shouldBe 2
      bData(0).value shouldBe 1
      bData(1).value shouldBe 2

      val aData = chart.data(chart.legend.indexOf("a"))
      aData.size shouldBe 4
      aData(0).value shouldBe 5

      val dData = chart.data(chart.legend.indexOf("d"))
      dData.size shouldBe 1
      dData(0).value.isNaN shouldBe true
    }

    "compute hash rate per currencie correctly" in {
      val aggregated = ChartService.aggregateCurrenciesByTime(measures, 2)
      val chart = service.computeHashratePerCurrencie(aggregated)

      chart.head.data.map(_.value) shouldBe Seq(0.5, 1, 0.5, 0.5, 1, 0.5)
    }

    "compute shares per currencie correctly" in {
      val aggregated = ChartService.aggregateCurrenciesByTime(measures, 2)
      val chart = service.computeSharesPerCurrencie(aggregated)

      chart(0).data(0).map(_.value) shouldBe Seq(3, 6, 3, 3, 6, 3)
      chart(0).data(1).map(_.value) shouldBe Seq(1, 2, 1, 1, 2, 1)
      chart(0).data(2).map(_.value) shouldBe Seq(2, 4, 2, 2, 4, 2)
    }

    "compute temp per host per card correctly" in {
      val aggregated = ChartService.aggregateMeasuresByHost(measures)
      val chart = service.computeTempPerHostPerCard(aggregated)

      println(chart)
      val bData = chart.find(_.title.startsWith("b")).get
      bData.data(0).map(_.value) shouldBe Seq(1, 1)
      bData.data(1).map(_.value) shouldBe Seq(3)
    }

    "compute fan per host per card correctly" in {
      val aggregated = ChartService.aggregateMeasuresByHost(measures)
      val chart = service.computeFanPerHostPerCard(aggregated)

      println(chart)
      val bData = chart.find(_.title.startsWith("b")).get
      bData.data(0).map(_.value) shouldBe Seq(50, 60)
      bData.data(1).map(_.value) shouldBe Seq(70)
    }
  }
}
