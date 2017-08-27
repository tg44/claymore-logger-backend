package com.github.tg44.claymore.service

import com.github.tg44.claymore.repository.measures.{CurrencyInformation, Measure, MeasureRepo, StatisticData}
import com.github.tg44.claymore.repository.users.UserRepo
import com.github.tg44.claymore.utils.GeneralUtil
import scaldi.{Injectable, Injector}

import scala.concurrent.{ExecutionContextExecutor, Future}

class ChartService(implicit injector: Injector, ec: ExecutionContextExecutor) extends Injectable {

  val measureRepo = inject[MeasureRepo]
  val userRepo = inject[UserRepo]

  def getCharts(extId: String, from: Long, to: Long): FResp[Charts] = {
    userRepo.findUserByExtId(extId).flatMap {
      case Some(user) =>
        measureRepo.getMesuresInRange(user.keys.map(_.value), from, to).map(measures => Right(computeChartsData(measures)))
      case None => Future.successful(Left(NoEntityFound))
    }
  }

  private def computeChartsData(measures: Seq[Measure]) = {
    val aggregatedMeasuresByHost = ChartService.aggregateMeasuresByHost(measures)
    val aggregatedCurrenciesByTime = ChartService.aggregateCurrenciesByTime(measures)
    Charts(
      computeAvgTempPerHost(aggregatedMeasuresByHost),
      computeHashratePerCurrencie(aggregatedCurrenciesByTime),
      computeSharesPerCurrencie(aggregatedCurrenciesByTime),
      computeTempPerHostPerCard(aggregatedMeasuresByHost),
      computeFanPerHostPerCard(aggregatedMeasuresByHost)
    )
  }

  private[service] def computeHashratePerCurrencie(aggregatedCurrenciesByTime: Map[String, Map[Long, Seq[CurrencyInformation]]]) = {
    val data = aggregatedCurrenciesByTime.mapValues(_.map(createHashRateChartData).toList)

    mapToSingleLineChartSeq(data)
  }

  private[service] def computeSharesPerCurrencie(aggregatedCurrenciesByTime: Map[String, Map[Long, Seq[CurrencyInformation]]]): Seq[MultilineChart] = {
    val data: Map[String, (Iterable[ChartData], Iterable[ChartData], Iterable[ChartData])] =
      aggregatedCurrenciesByTime.mapValues(_.map(createSharesChartData).unzip3)

    data.map {
      case (key, value) =>
        MultilineChart(Seq(value._1.toList.sortBy(_.date), value._2.toList.sortBy(_.date), value._3.toList.sortBy(_.date)),
                       Seq("valid", "invalid", "rejected"),
                       key)
    }.toList
  }

  private[service] def computeAvgTempPerHost(aggregatedMeasuresByHost: Map[String, Seq[StatisticData]]): MultilineChart = {
    val data = aggregatedMeasuresByHost.mapValues(_.map(createTempAvgChartData).sortBy(_.date))

    MultilineChart(data.values.toList, data.keys.toList, "avgTemp")
  }

  /*private def computeRuntimePerHost(aggregatedMeasuresByHost: Map[String, Seq[StatisticData]]): Seq[SingleLineChart] = {
    val data = aggregatedMeasuresByHost.mapValues(_.map(createRuntimeChartData))

    mapToSingleLineChartSeq(data)
  }*/

  private[service] def computeTempPerHostPerCard(aggregatedMeasuresByHost: Map[String, Seq[StatisticData]]): Seq[MultilineChart] = {
    aggregatedMeasuresByHost
      .mapValues(_.map(createTempChartData))
      .map {
        case (k, v) =>
          MultilineChart(GeneralUtil.transpose(v), v.zipWithIndex.map { case (_, i) => s"card$i" }, k + " temp/card")
      }
      .toList
  }

  private[service] def computeFanPerHostPerCard(aggregatedMeasuresByHost: Map[String, Seq[StatisticData]]): Seq[MultilineChart] = {
    aggregatedMeasuresByHost
      .mapValues(_.map(createFanChartData))
      .map {
        case (k, v) =>
          MultilineChart(GeneralUtil.transpose(v), v.zipWithIndex.map { case (_, i) => s"card$i" }, k + " fan/card")
      }
      .toList
  }

  private def mapToSingleLineChartSeq(map: Map[String, Seq[ChartData]]) = {
    map.map { case (title, data) => SingleLineChart(data.sortBy(_.date), title) }.toList
  }

  private def createTempAvgChartData(data: StatisticData): ChartData = {
    ChartData(GeneralUtil.convertTimeStampToChartString(data.timeStamp), data.tempsPerCard.sum / data.tempsPerCard.size)
  }

  private def createRuntimeChartData(data: StatisticData): ChartData = {
    ChartData(GeneralUtil.convertTimeStampToChartString(data.timeStamp), data.runTimeInMins / 60L / 24L)
  }

  private def createTempChartData(data: StatisticData): Seq[ChartData] = {
    data.cards.map(stat => ChartData(GeneralUtil.convertTimeStampToChartString(data.timeStamp), stat.temp)).sortBy(_.date)
  }

  private def createFanChartData(data: StatisticData): Seq[ChartData] = {
    data.cards.map(stat => ChartData(GeneralUtil.convertTimeStampToChartString(data.timeStamp), stat.fan))
  }

  private def createHashRateChartData(rawData: (Long, Seq[CurrencyInformation])): ChartData = {
    ChartData(GeneralUtil.convertTimeStampToChartString(rawData._1), rawData._2.map(_.sumHR).sum)
  }

  private def createSharesChartData(rawData: (Long, Seq[CurrencyInformation])): (ChartData, ChartData, ChartData) = {
    (
      ChartData(GeneralUtil.convertTimeStampToChartString(rawData._1), rawData._2.map(_.shares).sum),
      ChartData(GeneralUtil.convertTimeStampToChartString(rawData._1), rawData._2.map(_.invalidShares).sum),
      ChartData(GeneralUtil.convertTimeStampToChartString(rawData._1), rawData._2.map(_.sharesRejected).sum)
    )
  }
}

object ChartService {
  private[service] def aggregateCurrenciesByTime(measures: Seq[Measure], frame: Long = 300): Map[String, Map[Long, Seq[CurrencyInformation]]] = {
    val lower = measures.minBy(_.fromTimeStamp).fromTimeStamp
    val measureToMax = measures.maxBy(_.toTimeStamp).toTimeStamp
    val upper = if (measureToMax > GeneralUtil.nowInUnix) GeneralUtil.nowInUnix else measureToMax

    val timestamps = GeneralUtil.generateTimeStamps(lower, upper, frame)

    measures
      .flatMap(_.data)
      .flatMap(statData => statData.currencyInformations.map(currInfo => (currInfo.currency, statData.timeStamp, currInfo)))
      .foldLeft(Map[String, Seq[(Long, CurrencyInformation)]]()) {
        case (acc, (currency, ts, info)) =>
          acc + (currency -> (acc.getOrElse(currency, Seq.empty[(Long, CurrencyInformation)]) ++ Seq((ts, info))))
      }
      .map {
        case (currency, timeMeasureList) =>
          currency -> aggregateByTime(timeMeasureList, timestamps)
      }
  }

  private[service] def aggregateMeasuresByHost(measures: Seq[Measure]): Map[String, Seq[StatisticData]] = {
    measures
      .map(measure => measure.data.groupBy(mData => mData.endpointName))
      .foldLeft(Map[String, Seq[StatisticData]]()) {
        case (acc, hostDataMap) =>
          acc ++ hostDataMap.map {
            case (hostName, statisticDatas) =>
              hostName -> (statisticDatas ++ acc.getOrElse(hostName, Seq.empty[StatisticData]))
          }
      }
      .mapValues(dataList => dataList.sortBy(data => data.timeStamp))
  }

  private def aggregateByTime(rawData: Seq[(Long, CurrencyInformation)], timeStamps: Seq[Long]): Map[Long, Seq[CurrencyInformation]] = {
    rawData.foldLeft(Map[Long, Seq[CurrencyInformation]]()) {
      case (acc, element) =>
        val convertedTs: Long = timeStamps.foldLeft(timeStamps.head)((a, c) => if (c <= element._1) c else a)
        acc + (convertedTs -> (acc.getOrElse(convertedTs, Seq.empty[CurrencyInformation]) ++ Seq(element._2)))
    }
  }
}

case class Charts(
    avgTempPerHost: MultilineChart,
    currencyHashrateCharts: Seq[SingleLineChart],
    sharesPerCurrencies: Seq[MultilineChart],
    tempPerCardPerHost: Seq[MultilineChart],
    fanPerCardPerHost: Seq[MultilineChart]
)

case class SingleLineChart(
    data: Seq[ChartData],
    title: String
)

case class MultilineChart(
    data: Seq[Seq[ChartData]],
    legend: Seq[String],
    title: String
)

case class ChartData(
    date: String,
    value: Double
)
