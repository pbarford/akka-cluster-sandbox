package com.pjb.sandbox.actors

import akka.actor.{ActorLogging, Actor, Props}
import akka.contrib.pattern.{ClusterSharding, ShardRegion}

object Journal {
  val shardName = "journal"

  def props():Props =
    Props(new Journal())

  val idExtractor: ShardRegion.IdExtractor = {
    case msg: Message => (idFromMessage(msg), msg)
  }

  val shardResolver: ShardRegion.ShardResolver = msg => msg match {
    case m: Message => s"journal_${(math.abs(idFromMessage(m).hashCode) % 100)}"
  }
  private def idFromMessage(msg:Message):String = {
    msg.key match {
      case (Some(dest), eventId) => s"$dest:$eventId"
      case (None, eventId) => eventId.toString
    }
  }
}

class Journal() extends Actor with ActorLogging {

  log.info(s"*******   journal [${self.path.name}]")
  val latestStateRegion = ClusterSharding(context.system).shardRegion(LatestState.shardName)

  override def receive: Receive = {
    case msg:Message =>
      log.info(s"******** msg received ${msg.data} ")
      latestStateRegion.forward(msg)
      //sender() ! AckMsg(msg.seqNo)
    case _ =>
  }
}
