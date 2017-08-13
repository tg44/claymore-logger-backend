package com.github.tg44.claymore.service

import com.github.tg44.claymore.repository.measures.{Measure, MeasureRepo, StatisticData}
import com.github.tg44.claymore.utils.GeneralUtil
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContextExecutor

class ChartService(implicit injector: Injector, ec: ExecutionContextExecutor) extends Injectable {

  val measureRepo = inject[MeasureRepo]

  def getCharts(extId: String, from: Long, to: Long): FResp[Charts] = {
    measureRepo.getMesuresInRange(extId, from, to).map(measures => Right(computeChartsData(measures)))
  }

  private def computeChartsData(measures: Seq[Measure]) = {
    val aggregatedMeasuresByHost = aggregateMeasuresByHost(measures)
    Charts(
      computeAvgTempPerHost(aggregatedMeasuresByHost),
      computeRuntimePerHost(aggregatedMeasuresByHost),
      Seq(),
      Seq(),
      computeTempPerHostPerCard(aggregatedMeasuresByHost),
      computeFanPerHostPerCard(aggregatedMeasuresByHost)
    )
  }

  private def computeAvgTempPerHost(aggregatedMeasuresByHost: Map[String, Seq[StatisticData]]): MultilineChart = {
    val data = aggregatedMeasuresByHost.mapValues(_.map(createTempAvgChartData))

    MultilineChart(data.values.toList, data.keys.toList, "avgTemp")
  }

  private def computeRuntimePerHost(aggregatedMeasuresByHost: Map[String, Seq[StatisticData]]): Seq[SingleLineChart] = {
    val data = aggregatedMeasuresByHost.mapValues(_.map(createRuntimeChartData))

    mapToSingleLineChartSeq(data)
  }

  private def computeTempPerHostPerCard(aggregatedMeasuresByHost: Map[String, Seq[StatisticData]]): Seq[MultilineChart] = {
    aggregatedMeasuresByHost
      .mapValues(_.map(createTempChartData))
      .map {
        case (k, v) =>
          MultilineChart(v, v.zipWithIndex.map { case (_, i) => s"card$i" }, k + " temp/card")
      }
      .toList
  }

  private def computeFanPerHostPerCard(aggregatedMeasuresByHost: Map[String, Seq[StatisticData]]): Seq[MultilineChart] = {
    aggregatedMeasuresByHost
      .mapValues(_.map(createFanChartData))
      .map {
        case (k, v) =>
          MultilineChart(v, v.zipWithIndex.map { case (_, i) => s"card$i" }, k + " fan/card")
      }
      .toList
  }

  private def aggregateMeasuresByHost(measures: Seq[Measure]): Map[String, Seq[StatisticData]] = {
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

  private def mapToSingleLineChartSeq(map: Map[String, Seq[ChartData]]) = {
    map.map { case (title, data) => SingleLineChart(data, title) }.toList
  }

  private def createTempAvgChartData(data: StatisticData): ChartData = {
    ChartData(GeneralUtil.convertTimeStampToChartString(data.timeStamp), data.tempsPerCard.sum / data.tempsPerCard.size)
  }

  private def createRuntimeChartData(data: StatisticData): ChartData = {
    ChartData(GeneralUtil.convertTimeStampToChartString(data.timeStamp), data.runTimeInMins / 60L / 24L)
  }

  private def createTempChartData(data: StatisticData): Seq[ChartData] = {
    data.cards.map(stat => ChartData(GeneralUtil.convertTimeStampToChartString(data.timeStamp), stat.temp))
  }

  private def createFanChartData(data: StatisticData): Seq[ChartData] = {
    data.cards.map(stat => ChartData(GeneralUtil.convertTimeStampToChartString(data.timeStamp), stat.fan))
  }
}

case class Charts(
    avgTempPerHost: MultilineChart,
    runtimePerHost: Seq[SingleLineChart], //in days
    currencyHashrateCharts: Seq[MultilineChart],
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
