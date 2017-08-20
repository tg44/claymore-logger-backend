package com.github.tg44.claymore.utils

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

object GeneralUtil {

  // 10000 seconds is means nearly 3h goes to 1 doc
  // if one statdata is ~2kb, 1 user has 50 rig and send 1 measure/min
  // the data in one mongo doc will be < 2kb*50*180 = 17,57Mb  >>16Mb mongo limit
  // with this period number 50 rig and 1 measure/min is a hard limit!!!
  def generatePeriod(time: Long, frame: Long = 10000): (Long, Long) = {
    (
      (time - 1) / frame * frame + 1,
      (((time - 1) / frame) + 1) * frame
    )
  }

  def generateTimeStamps(lowerBound: Long, upperBound: Long, frame: Long): Seq[Long] = {
    val lower = (lowerBound - 1) / frame * frame
    val upper = (((upperBound - 1) / frame) + 1) * frame
    lower to upper by frame
  }

  def convertTimeStampToChartString(ts: Long) = {
    val date = new Date(ts * 1000L)
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
    sdf.format(date)
  }

  def nowInUnix: Long = System.currentTimeMillis / 1000
  def uuid = java.util.UUID.randomUUID.toString
}
