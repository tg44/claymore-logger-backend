package com.github.tg44.claymore.service

import com.github.tg44.claymore.api.service.StatisticDataDto
import com.github.tg44.claymore.config.Config
import com.github.tg44.claymore.repository.measures.{MeasureRepo, StatisticData}
import com.github.tg44.claymore.utils.GeneralUtil
import scaldi.{Injectable, Injector}

import scala.concurrent.{ExecutionContextExecutor, Future}

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
      statData.data.eth,
      statData.data.dcr,
      statData.data.tempsPerCard,
      statData.data.fansPerCard,
      statData.data.cards
    )
  }
}
