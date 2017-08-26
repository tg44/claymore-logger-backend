package com.github.tg44.claymore.service

import com.github.tg44.claymore.api.service.StatisticDataDto
import com.github.tg44.claymore.config.Config
import com.github.tg44.claymore.repository.measures.{CurrencyInformation, Measure, MeasureRepo, StatisticData}
import com.github.tg44.claymore.utils.GeneralUtil
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContextExecutor

class StatisticDataService(implicit injector: Injector, ec: ExecutionContextExecutor) extends Injectable {

  val measureRepo = inject[MeasureRepo]
  val config = inject[Config]

  def saveData(extId: String, statData: StatisticDataDto): FResp[Long] = {
    val time = GeneralUtil.nowInUnix
    measureRepo
      .saveNewMeasure(extId, createStatisticDataFromDto(time, statData), GeneralUtil.generatePeriod(time))
      .map(_ => Right(config.CLIENT.defaultWaitTimeInSecs))
  }

  def createStatisticDataFromDto(time: Long, statData: StatisticDataDto): StatisticData = {
    StatisticData(
      time,
      statData.name,
      statData.remoteAddress,
      statData.remotePort,
      statData.data.minerVersion,
      statData.data.runTimeInMins,
      statData.data.currencyInformations,
      statData.data.tempsPerCard,
      statData.data.fansPerCard,
      statData.data.cards
    )
  }

  def getLastMeasures(extId: String, from: Long, to: Long): FResp[StatisticDataWrapper] = {
    measureRepo.getMesuresInRange(extId, from, to).map(measures => Right(computeLatestDataLists(measures)))
  }

  private def computeLatestDataLists(measures: Seq[Measure]): StatisticDataWrapper = {
    val aggregatedMeasuresByHost: Map[String, Seq[StatisticData]] = ChartService.aggregateMeasuresByHost(measures)
    val aggregatedCurrenciesByTime: Map[String, Map[Long, Seq[CurrencyInformation]]] = ChartService.aggregateCurrenciesByTime(measures)

    val latestDataPerHost: Map[String, StatisticData] = aggregatedMeasuresByHost.mapValues(_.maxBy(_.timeStamp))
    val summarizedDataByCurrencies: Map[String, List[(Long, CurrencyInformation, Int)]] = aggregatedCurrenciesByTime
      .mapValues(
        _.mapValues(
          sumCurrencieInformation
        )
      )
      .map {
        case (key, value) =>
          key -> value
            .map {
              case (ts, (ci, count)) =>
                (ts, ci, count)
            }
            .toList
            .sortBy(_._1)
      }

    //this code assumes 5min chunks and 14day data, later this can be more generic
    val summarizedCurrencies24h: Map[String, (CurrencyInformation, Int)] = summarizeCurrenciesToDaysFrame(summarizedDataByCurrencies, 1)
    val summarizedCurrencies3d = summarizeCurrenciesToDaysFrame(summarizedDataByCurrencies, 3)
    val summarizedCurrencies7d = summarizeCurrenciesToDaysFrame(summarizedDataByCurrencies, 7)
    val summarizedCurrencies14d = summarizeCurrenciesToDaysFrame(summarizedDataByCurrencies, 14)

    StatisticDataWrapper(
      latestDataPerHost,
      summarizedCurrencies24h,
      summarizedCurrencies3d,
      summarizedCurrencies7d,
      summarizedCurrencies14d
    )
  }

  private def summarizeCurrenciesToDaysFrame(summarizedDataByCurrencies: Map[String, List[(Long, CurrencyInformation, Int)]], days: Int) = {
    summarizedDataByCurrencies.mapValues { list =>
      val data = list.reverse.take(days * 24 * 60 / 5)
      (sumCurrencieInformation(data.map(_._2))._1, data.map(_._3).sum)
    }
  }

  private def sumCurrencieInformation(ciList: Seq[CurrencyInformation]) = {
    (ciList.fold(CurrencyInformation("", 0, 0, 0, 0, 0, Nil, "")) {
      case (acc, data) =>
        data.copy(
          sumHR = data.sumHR + acc.sumHR,
          shares = data.shares + acc.shares,
          sharesRejected = data.sharesRejected + acc.sharesRejected,
          invalidShares = data.invalidShares + acc.sharesRejected,
          poolSwitches = data.poolSwitches + acc.poolSwitches
        )
    }, ciList.size)
  }
}

case class StatisticDataWrapper(
    latestDataPerHost: Map[String, StatisticData],
    summarizedCurrencies24h: Map[String, (CurrencyInformation, Int)],
    summarizedCurrencies3d: Map[String, (CurrencyInformation, Int)],
    summarizedCurrencies7d: Map[String, (CurrencyInformation, Int)],
    summarizedCurrencies14d: Map[String, (CurrencyInformation, Int)]
)
