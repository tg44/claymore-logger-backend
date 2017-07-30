package com.github.tg44.claymore.utils

object GeneralUtil {

  // 10000 seconds is means nearly 3h goes to 1 doc
  // if one statdata is ~2kb, 1 user has 50 rig and send 1 measure/min
  // the data in one mongo doc will be < 2kb*50*180 = 17,57Mb  >>16Mb mongo limit
  // with this period number 50 rig and 1 measure/min is a hard limit!!!
  def generatePeriod(time: Long): (Long, Long) = {
    (
      (time - 1) / 10000 * 10000 + 1,
      (((time - 1) / 10000) + 1) * 10000
    )
  }

  def nowInUnix: Long = System.currentTimeMillis / 1000
  def uuid = java.util.UUID.randomUUID.toString
}
