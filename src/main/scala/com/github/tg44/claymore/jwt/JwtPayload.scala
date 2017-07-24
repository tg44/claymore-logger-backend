package com.github.tg44.claymore.jwt

case class JwtPayload(userId: String, authorities: List[String] = List.empty[String])
