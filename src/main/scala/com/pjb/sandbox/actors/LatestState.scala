package com.pjb.sandbox.actors

import akka.actor.{ActorLogging, Actor, Props}
import akka.contrib.pattern.ShardRegion

object LatestState {
  val shardName = "latestState"

  def props():Props =
    Props(new LatestState)

  val idExtractor: ShardRegion.IdExtractor = {
    case msg: Message => (idFromMessage(msg), msg)
  }

  val shardResolver: ShardRegion.ShardResolver = msg => msg match {
    case m: Message => s"latestState_${(math.abs(idFromMessage(m).hashCode) % 100)}"
  }
  private def idFromMessage(msg:Message):String = {
    msg.key match {
      case (Some(dest), eventId) => s"$dest:$eventId"
      case (None, eventId) => eventId.toString
    }
  }
}

class LatestState() extends Actor with ActorLogging {

  log.info(s"*******   latest state [${self.path.name}]")

  override def receive: Receive = {
    case msg:Message =>
      log.info(s"******** msg received ${msg.data} ")
      sender() ! AckMsg(msg.deliveryInfo)
    case _ =>
  }
}

