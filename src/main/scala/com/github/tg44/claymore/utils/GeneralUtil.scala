package com.github.tg44.claymore.utils

object GeneralUtil {
  def nowInUnix: Long = System.currentTimeMillis / 1000
  def uuid = java.util.UUID.randomUUID.toString
}
