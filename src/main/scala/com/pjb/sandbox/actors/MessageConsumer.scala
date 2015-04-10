package com.pjb.sandbox.actors

import akka.actor.{ActorLogging, Props, Actor}
import akka.contrib.pattern.ClusterSharding
import com.pjb.sandbox.util.Merger
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client._

import scala.collection.JavaConverters

object MessageConsumer {
  def props(rabbitConnectionFactory: () => Connection,
            queue:String):Props = Props(new MessageConsumer(rabbitConnectionFactory, queue))

}

class MessageConsumer(rabbitConnectionFactory: () => Connection,
                      queue:String) extends Actor with ActorLogging with Consumer {

  log.info(s"******** consumer [${self.path.name}]")
  val journalRegion = ClusterSharding(context.system).shardRegion(PersistentJournal.shardName)
  init()

  def init(): Unit = {
    val hostName:String = {
      context.self.path.address.host match {
        case Some(name) => name
        case None => "unknown"
      }
    }
    val channel:Channel = rabbitConnectionFactory().createChannel()
    channel.basicConsume(queue, false, consumerTagFor(queue, hostName), this)
    context.become(connected(channel))
  }

  def generateMessage(basicProps: BasicProperties, envelope: Envelope, data: Array[Byte]): Message = {
    val headers: Map[String, AnyRef] = JavaConverters.mapAsScalaMapConverter(basicProps.getHeaders).asScala.toMap
    Message(keyFromHeaders(headers),
      DeliveryInfo(System.currentTimeMillis(), envelope.getDeliveryTag),
      if(headers.contains(sequenceNumberHeader)) java.lang.Integer.parseInt(headers(sequenceNumberHeader).toString) else 0,
      getStatusFromHeaders(headers),
      Merger.toJson(headers),
      new String(data, "UTF-8"))
  }

  def keyFromHeaders(headers: Map[String, AnyRef]):MessageKey = {
    headers.contains(destinationHeader) match {
      case true => (Some(headers(destinationHeader).toString),java.lang.Long.parseLong(headers.getOrElse(eventIdHeader, 0).toString))
      case false => (None, java.lang.Long.parseLong(headers.getOrElse(eventIdHeader, 0).toString))
    }
  }

  def getStatusFromHeaders(headers:Map[String,AnyRef]):Option[String] = {
    headers.contains(destinationHeader) match {
      case true =>
        if(headers.contains(suppressedPayloadHeader))
          Some(suppressedStatus)
        else if(headers.contains(ghostMessageHeader))
          Some(sentGhostStatus)
        else if(headers.contains(duplicateMessageHeader))
          Some(duplicateStatus)
        else
          Some(sentStatus)

      case false => None
    }
  }

  override def handleConsumeOk(consumerTag: String): Unit = {}

  override def handleRecoverOk(consumerTag: String): Unit = {}

  override def handleCancel(consumerTag: String): Unit = {}

  override def handleCancelOk(consumerTag: String): Unit = {}

  override def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException): Unit = {}

  override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]): Unit = {
    self ! generateMessage(properties, envelope, body)
  }

  override def receive: Receive = notConnected

  def notConnected:Receive = {
    case _ => log.info("not connected")
  }

  def connected(channel:Channel): Receive = {
    case msg:Message =>
      log.info(s"******** msg received ")
      journalRegion.forward(msg)

    case AckMsg(deliveryInfo:DeliveryInfo) =>
      log.info(s"ack --> ${deliveryInfo.deliveryTag} took ${System.currentTimeMillis() - deliveryInfo.consumedAt} m/s")
      channel.basicAck(deliveryInfo.deliveryTag, false)

    case _ => log.info("msg received")
  }

  def consumerTagFor(queue:String, hostName:String):String = s"$queue-$hostName"
}
