package com.github.tg44.claymore.api

package object service {

  case class ParsedStatisticResponse(
      minerVersion: String,
      runTimeInMins: Double,
      eth: CurrencyInformation,
      dcr: CurrencyInformation,
      tempsPerCard: Seq[Double],
      fansPerCard: Seq[Double],
      cards: Seq[CardStatistic]
  )

  case class CurrencyInformation(
      sumHR: Double,
      shares: Int,
      sharesRejected: Int,
      invalidShares: Int,
      poolSwitches: Int,
      perCardHR: Seq[Double],
      currentPool: String
  )

  case class CardStatistic(
      ethHR: Double,
      dcrHR: Double,
      temp: Double,
      fan: Double
  )

  case class StatisticDataDto(
      name: String,
      remoteAddress: String,
      remotePort: Int,
      data: ParsedStatisticResponse
  )

  case class ApiSecurityDto(
      secret: String
  )

}
