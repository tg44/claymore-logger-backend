package com.github.tg44.claymore.api

import com.github.tg44.claymore.repository.users.ApiKey

package object keyhandling {
  case class ApiKeyList(list: Seq[ApiKey])
  case class Name(name: String)
}
