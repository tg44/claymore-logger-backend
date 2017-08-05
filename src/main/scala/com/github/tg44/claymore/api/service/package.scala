package com.github.tg44.claymore.api

import com.github.tg44.claymore.repository.measures.{CardStatistic, CurrencyInformation}

package object service {

  case class ParsedStatisticResponse(
      minerVersion: String,
      runTimeInMins: Double,
      currencyInformations: Seq[CurrencyInformation],
      tempsPerCard: Seq[Double],
      fansPerCard: Seq[Double],
      cards: Seq[CardStatistic]
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
