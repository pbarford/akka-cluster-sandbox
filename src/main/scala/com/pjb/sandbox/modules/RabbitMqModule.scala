package com.pjb.sandbox.modules

import com.rabbitmq.client.{ShutdownSignalException, ConnectionFactory, Connection, ShutdownListener}

trait RabbitMqModule extends ShutdownListener {

  this: ConfigModule =>

  private var internalConnection:Option[Connection] = None

  lazy val internalCf:ConnectionFactory = {
    val cf = new ConnectionFactory()
    cf.setUsername(rabbitUser)
    cf.setPassword(rabbitPassword)
    cf.setRequestedHeartbeat(rabbitHeartBeatInSec)
    cf.setConnectionTimeout(rabbitConnTimeoutInMs)
    cf.setVirtualHost(rabbitVhost)
    cf.setHost(rabbitHost)
    cf.setPort(rabbitPort)
    if(rabbitUseSsl) cf.useSslProtocol()
    cf
  }

  private def initialiseConnection:Connection = {
    val conn:Connection = {
      if (rabbitAddresses.nonEmpty)
        internalCf.newConnection(rabbitAddresses)
      else
        internalCf.newConnection()
    }
    conn.addShutdownListener(this)
    internalConnection = Some(conn)
    conn
  }

  def rabbitConnectionFactory:() => Connection = { () =>
    internalConnection match {
      case None => initialiseConnection
      case Some(connection) => connection
    }
  }

  override def shutdownCompleted(cause: ShutdownSignalException): Unit = initialiseConnection
}