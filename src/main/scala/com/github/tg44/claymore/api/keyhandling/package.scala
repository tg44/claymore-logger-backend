package com.github.tg44.claymore.api

import com.github.tg44.claymore.repository.users.ApiKey

package object keyhandling {
  case class ApiKeyList(list: Seq[ApiKey])
  case class Name(name: String)
  case class ImportKey(fromUser: String, keyName: String, keySecret: String)
}
