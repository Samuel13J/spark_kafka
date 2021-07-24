package com.jaywong.redis

import redis.clients.jedis.Jedis

/**
 * @author wangjie
 * @create 2021-05-06 17:44
 */
object GetRedisClient {
  def getRedisClient(redisUgi: String): Jedis = {
    val port = 6379 //端口号
    val ip = "127.0.0.1"
    //    val (redisHost, redisPort) = getConfig()
    val redisClient = new Jedis(ip, port)
    redisClient.auth(redisUgi)
    redisClient
  }

}
