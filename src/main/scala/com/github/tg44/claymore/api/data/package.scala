package com.github.tg44.claymore.api

import com.github.tg44.claymore.repository.measures.{CurrencyInformation, StatisticData}
import com.github.tg44.claymore.service.StatisticDataWrapper

package object data {

  case class StatisticDataresponseDto(latestDataPerHost: Map[String, StatisticData],
                                      summarizedCurrencies24h: Map[String, CurrencyData],
                                      summarizedCurrencies3d: Map[String, CurrencyData],
                                      summarizedCurrencies7d: Map[String, CurrencyData],
                                      summarizedCurrencies14d: Map[String, CurrencyData])

  object StatisticDataresponseDto {
    def apply(data: StatisticDataWrapper): StatisticDataresponseDto = StatisticDataresponseDto(
      data.latestDataPerHost,
      data.summarizedCurrencies24h.mapValues(CurrencyData(_)),
      data.summarizedCurrencies3d.mapValues(CurrencyData(_)),
      data.summarizedCurrencies7d.mapValues(CurrencyData(_)),
      data.summarizedCurrencies14d.mapValues(CurrencyData(_))
    )
  }

  case class CurrencyData(
      sumHR: Double,
      shares: Int,
      sharesRejected: Int,
      invalidShares: Int,
      poolSwitches: Int,
      measurementCount: Int
  )

  object CurrencyData {
    def apply(data: (CurrencyInformation, Int)): CurrencyData = CurrencyData(
      data._1.sumHR,
      data._1.shares,
      data._1.sharesRejected,
      data._1.invalidShares,
      data._1.poolSwitches,
      data._2
    )
  }

}
