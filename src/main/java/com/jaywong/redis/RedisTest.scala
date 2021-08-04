package com.jaywong.redis

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel

import redis.clients.jedis.Response

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * @author wangjie
  **@create 2021-05-06 18:08
 */
object RedisTest {
  var inputPath = "D:\\redis.txt"
  var redisUgi = "123456"
  var keyList = "b,d,e"
  var mode = ""


  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("RedisTest")
      .getOrCreate()

//    init(args)

    if ("delete".equals(mode)) {
      delData(spark)
    } else if ("insert".equals(mode)) {
      putData(spark)
    } else {
      val redisUgiBc = spark.sparkContext.broadcast(redisUgi)
      val jedisWrite = GetRedisClient.getRedisClient(redisUgiBc.value)

      val arr = keyList.split(",")
      arr.foreach(x => println(x + ":" + jedisWrite.hget(x, "field_name")))
    }
  }
  //批量插入数据
  def putData(spark: SparkSession): Unit = {
    val rdd = spark.sparkContext
      .textFile(inputPath)
      .persist(StorageLevel.MEMORY_AND_DISK)

    println("count:" + rdd.count())
    println(rdd.take(5).mkString("\n"))

    val redisUgiBc = spark.sparkContext.broadcast(redisUgi)
    rdd.repartition(400)
      .foreachPartition(iter => {
        val users = iter.map(x => {
          val item = x.split("\t")
          val userid = item(0)
          val userinfo = item(1)
          (userid, userinfo)
        })
        putDataPartition(users, redisUgiBc, 3600 * 24 * 2)
      })
  }

//并行删除数据
  def delData(spark: SparkSession): Unit = {
    val rdd = spark.sparkContext
      .textFile(inputPath)
      .persist(StorageLevel.MEMORY_AND_DISK)

    println("count:" + rdd.count())
    println(rdd.take(5).mkString("\n"))

    val redisUgiBc = spark.sparkContext.broadcast(redisUgi)
    rdd.repartition(400)
      .foreachPartition(iter => {
        val users = iter.map(x => {
          val item = x.split("\t")
          val userid = item(0)
          val userinfo = item(1)
          (userid, userinfo)
        })
        delDataPartition(users, redisUgiBc)
      })
  }

  /**
   * 将hdfs上面的内容读取后写入redis
   *
   */
  def putDataPartition(dataIt: Iterator[(String, String)], redisUgiBc: Broadcast[String], expireTime: Int = 3600 * 24 * 2): Unit = {
    val jedisClient = GetRedisClient.getRedisClient(redisUgiBc.value)
    val dataList = dataIt.toArray
    val batchNum = 30
    val nStep = math.ceil(dataList.size / batchNum.toDouble).toInt

    for (index <- 0 to nStep) {
      val lowerIndex = batchNum * index
      val upperIndex =
        if (lowerIndex + batchNum >= dataList.size) dataList.size
        else batchNum * (index + 1)
      val batchData = dataList.slice(lowerIndex, upperIndex)
      val batchDataSize = 0
      val pipline = jedisClient.pipelined()

      batchData.foreach(data => {
        val dataKey = data._1
        val dataValue = data._2
        pipline.hset(dataKey, "field_name", dataValue)
        pipline.expire(dataKey, expireTime)
      })

      pipline.sync()
    }
  }

  def delDataPartition(dataIt: Iterator[(String, String)], redisUgiBc: Broadcast[String]) : Unit = {
    val jedisClient = GetRedisClient.getRedisClient(redisUgiBc.value)
    val dataList = dataIt.toArray
    val batchNum = 50
    val nStep = math.ceil(dataList.size / batchNum.toDouble).toInt

    for (index <- 0 to nStep) {
      val lowerIndex = batchNum * index
      val upperIndex =
        if (lowerIndex + batchNum >= dataList.size) dataList.size
        else batchNum * (index + 1)
      val batchData = dataList.slice(lowerIndex, upperIndex)

      batchData.foreach(data => {
        val dataKey = data._1
        jedisClient.del(dataKey)
      })
    }
  }

  /**
   * 读取Redis测试
   */
  def getFromRedis(spark: SparkSession): Unit = {
    val keylist = ArrayBuffer()
    spark.sparkContext.textFile(inputPath)
      .take(100)
//      .foreach(key => keylist.append(key))

    val jedisWrite = GetRedisClient.getRedisClient(redisUgi)
    val response: scala.collection.mutable.Map[String, Response[String]] = mutable.HashMap()
    val pipeline = jedisWrite.pipelined()
    keylist.foreach(key => {
      val value = pipeline.hget(key, "field_name")
      response.put(key, value)
    })
    val res = pipeline.syncAndReturnAll() //这里只是返回value，没有key的信息

    println("response:")
    response.take(120).foreach(x => println(x._1 + ":" + x._2.get()))
    println("res:")
    res.toArray().take(120).map(x => x.asInstanceOf[String]).foreach(println)

  }
}
