package com.pjb.sandbox.modules

import java.net.InetAddress

import com.rabbitmq.client
import com.rabbitmq.client.Address
import com.typesafe.config.ConfigFactory

trait ConfigModule {
  val configuration = ConfigFactory.load()

  def hostName:String = InetAddress.getLocalHost().getHostName()
  def rabbitUser:String = "guest"
  def rabbitPassword:String = "guest"
  def rabbitVhost:String = "/"
  def rabbitAddresses:Array[Address] = Array.empty[client.Address]
  def rabbitUseSsl:Boolean = false
  def rabbitHeartBeatInSec:Int = 30
  def rabbitConnTimeoutInMs:Int = 60
  def rabbitHost:String = "127.0.0.1"
  def rabbitPort:Int = 5672

  def cassandraContactPoints = "127.0.0.1"
  def cassandraUsername = ""
  def cassandraPassword = ""
  def cassandraKeyspace = "dm_keyspace_dev"
  def cassandraUseSSL = false
}
