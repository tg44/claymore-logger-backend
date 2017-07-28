package com.github.tg44.claymore.service

import com.github.tg44.claymore.api.service.StatisticDataDto
import com.github.tg44.claymore.repository.measures.MeasureRepo
import scaldi.{Injectable, Injector}

class StatisticDataService(implicit injector: Injector) extends Injectable {

  val measureRepo = inject[MeasureRepo]

  def saveData(statData: StatisticDataDto): FResp[Long] = ???
}
