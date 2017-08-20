package com.github.tg44.claymore.api.data

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.tg44.claymore.service.{ChartData, Charts, MultilineChart, SingleLineChart}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait ChartsJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val chartsJsonFormatter: RootJsonFormat[Charts] = jsonFormat5(Charts)
  implicit val multiLineChartsJsonFormatter: RootJsonFormat[MultilineChart] = jsonFormat3(MultilineChart)
  implicit val singleLineChartsJsonFormatter: RootJsonFormat[SingleLineChart] = jsonFormat2(SingleLineChart)
  implicit val chartDataJsonFormatter: RootJsonFormat[ChartData] = jsonFormat2(ChartData)
}
