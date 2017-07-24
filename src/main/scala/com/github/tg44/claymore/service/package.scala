package com.github.tg44.claymore

import scala.concurrent.Future

package object service {
  type FResp[T] = Future[Either[ErrorResponse, T]]

  abstract class ErrorResponse()
}
