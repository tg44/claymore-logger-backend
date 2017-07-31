package com.github.tg44.claymore.service

import com.github.tg44.claymore.jwt.{Jwt, JwtPayload}
import com.github.tg44.claymore.repository.users.UserRepo
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContextExecutor

class AuthService(implicit injector: Injector, ec: ExecutionContextExecutor) extends Injectable {

  val userRepo = inject[UserRepo]
  val jwt = inject[Jwt]

  def authenticateWithApiKey(secret: String): FResp[String] = {

    userRepo.findUserByApiKey(secret).map { usr =>
      usr.fold[Resp[String]](
        Left(AuthenticationError())
      )(user => Right(jwt.encode(JwtPayload(user.extid))))
    }
  }

}
