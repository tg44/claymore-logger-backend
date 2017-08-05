package com.github.tg44.claymore

import scala.concurrent.Future
import org.json4s.DefaultFormats

package object service {
  type Resp[T] = Either[ErrorResponse, T]
  type FResp[T] = Future[Either[ErrorResponse, T]]

  implicit val jsonFormats = DefaultFormats

  abstract class ErrorResponse()
  case object AuthenticationError extends ErrorResponse
}
