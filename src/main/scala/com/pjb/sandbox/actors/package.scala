package com.pjb.sandbox

package object actors {

  val destinationHeader:String = "destination"
  val eventIdHeader:String = "eventId"
  val sequenceNumberHeader:String = "sequenceNumber"

  val suppressedPayloadHeader:String = "suppressedPayload"
  val ghostMessageHeader:String = "ghostMessage"
  val duplicateMessageHeader:String = "duplicateMessage"

  val sentStatus:String = "SENT"
  val sentGhostStatus:String = "SENT-GHOST"
  val suppressedStatus:String = "SUPPRESSED"
  val duplicateStatus:String = "DUPLICATE"

  val inboundMessagesTable:String = "inboundmessages"
  val outboundMessagesTable:String = "outboundmessages"
  val duplicateMessagesTable:String = "duplicatemessages"

  val sentSeqNoTable:String = "sentseqno"
  val eventActivityTable:String = "eventactivity"

  val ttlProperty:String = "x-message-ttl"

  case class AckMsg(deliveryInfo:DeliveryInfo)
  case class StartConsumer(queue:String)

  case class GetLatestState(key:CorrelationKey)

  case class EventState(key:CorrelationKey, uptoSeqNo:Int, data:String)

  case class DeliveryInfo(consumedAt:Long, deliveryTag:Long)
  case class Message(key:CorrelationKey,
                     deliveryInfo: DeliveryInfo,
                     seqNo:Int,
                     status: Option[String],
                     headers: String,
                     data:String)

  type CorrelationKey = (Option[String],Long)
}
