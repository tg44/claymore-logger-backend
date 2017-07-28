package com.github.tg44.claymore.service

import com.github.tg44.claymore.repository.users.UserRepo
import scaldi.{Injectable, Injector}

class AuthService(implicit injector: Injector) extends Injectable {

  val userRepo = inject[UserRepo]

  def authenticateWithApiKey(secret: String): FResp[String] = ???

}
