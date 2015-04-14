package com.pjb.sandbox.actors

import java.util

import akka.actor.{ActorLogging, Props, Actor}
import akka.contrib.pattern.ClusterSharding
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client._
import com.rabbitmq.client.impl.LongStringHelper
import org.json4s.JsonAST.{JString, JField, JObject}
import org.json4s.{CustomSerializer, DefaultFormats}
import org.json4s.native.Serialization.write

object MessageConsumer {
  def props(rabbitConnectionFactory: () => Connection,
            queue:String):Props = Props(new MessageConsumer(rabbitConnectionFactory, queue))

}

class MessageConsumer(rabbitConnectionFactory: () => Connection,
                      queue:String) extends Actor with ActorLogging with Consumer {
  implicit lazy val formats = DefaultFormats + new LongStringSerializer
  val journalTTL:Long = 60000
  val journalRegion = ClusterSharding(context.system).shardRegion(Journal.shardName)
  init()

  def init(): Unit = {
    val hostName:String = {
      context.self.path.address.host match {
        case Some(name) => name
        case None => "unknown"
      }
    }
    val queueArgs = new util.HashMap[String, Object]()
    queueArgs.put(ttlProperty, Long.box(journalTTL))
    val channel:Channel = rabbitConnectionFactory().createChannel()
    channel.queueDeclare(queue, false, false, false, queueArgs)
    channel.basicConsume(queue, false, consumerTagFor(queue, hostName), this)
    log.info(s"******** consumer [${self.path.name}]")
    context.become(connected(channel))
  }

  def generateMessage(basicProps: BasicProperties, envelope: Envelope, data: Array[Byte]): Message = {
    import scala.collection.JavaConverters
    val headers: Map[String, AnyRef] = JavaConverters.mapAsScalaMapConverter(basicProps.getHeaders).asScala.toMap
    val key = keyFromHeaders(headers)
    val seqNo = if(headers.contains(sequenceNumberHeader)) java.lang.Integer.parseInt(headers(sequenceNumberHeader).toString) else 0
    val status = getStatusFromHeaders(headers)
    val headersJson = write(headers)
    Message(key,
      DeliveryInfo(System.currentTimeMillis(), envelope.getDeliveryTag),
      seqNo,
      status,
      headersJson,
      new String(data, "UTF-8"))
  }

  def keyFromHeaders(headers: Map[String, AnyRef]):CorrelationKey = {
    headers.contains(destinationHeader) match {
      case true => (Some(headers(destinationHeader).toString), java.lang.Long.parseLong(headers(eventIdHeader).toString))
      case false => (None, java.lang.Long.parseLong(headers(eventIdHeader).toString))
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

  override def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException): Unit = {
    log.error(sig.getMessage)
  }


  override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]): Unit = {
    self ! generateMessage(properties, envelope, body)
  }

  override def receive: Receive = notConnected

  def notConnected:Receive = {
    case _ => log.info("******** not connected")
  }

  def connected(channel:Channel): Receive = {
    case msg:Message =>
      log.info(s"******** msg received ")
      journalRegion forward msg

    case AckMsg(deliveryInfo:DeliveryInfo) =>
      log.info(s"ack --> ${deliveryInfo.deliveryTag} took ${System.currentTimeMillis() - deliveryInfo.consumedAt} m/s")
      channel.basicAck(deliveryInfo.deliveryTag, false)

    case _ => log.info("msg received")
  }

  def consumerTagFor(queue:String, hostName:String):String = s"$queue-$hostName"

  //This is only used to convert to JSON - the from JSON implementation *is only provided as it is required*
  private class LongStringSerializer extends CustomSerializer[LongString](format => (
    { case JObject(JField("destination", JString(s)) :: Nil) => LongStringHelper.asLongString(s) },
    { case x: LongString => JString(x.toString) }))

}
