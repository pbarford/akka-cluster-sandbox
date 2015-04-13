package com.pjb.sandbox.actors

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ReceiveTimeout, ActorLogging, Props}
import akka.contrib.pattern.ShardRegion
import com.datastax.driver.core.Session
import scala.concurrent.duration._

object Journal {
  val shardName = "journal"

  def props(cassandraSession:() => Session):Props = Props(new Journal(cassandraSession))

  val idExtractor: ShardRegion.IdExtractor = {
    case msg: Message => (idFromMessageKey(msg.key), msg)
    case req: GetLatestState => (idFromMessageKey(req.key), req)
  }

  val shardResolver: ShardRegion.ShardResolver = msg => msg match {
    case m: Message => (math.abs(idFromMessageKey(m.key).hashCode) % 100).toString
    case r: GetLatestState => (math.abs(idFromMessageKey(r.key).hashCode) % 100).toString
  }
  private def idFromMessageKey(key:MessageKey):String = {
    key match {
      case (Some(dest), eventId) => s"$dest.$eventId"
      case (None, eventId) => eventId.toString
    }
  }
}

class Journal(cassandraSession:() => Session) extends Actor with ActorLogging {

  import akka.contrib.pattern.ShardRegion.Passivate
  context.setReceiveTimeout(1.minutes)

  val session = cassandraSession()
  val snapshot = context.actorOf(Snapshot.props(cassandraSession), "snapshot")

  val creationTime = System.currentTimeMillis()

  log.info("journal init")

  override def receive: Receive = {
    case msg:Message =>
      log.info(s"******** msg received ${msg.data} ")
      persistInCassandra(msg)
      snapshot forward  msg
      sender() ! AckMsg(msg.deliveryInfo)

    case req:GetLatestState =>
      log.info(s"******** forward latest state req")
      snapshot forward GetLatestState

    case ReceiveTimeout =>
      log.info(s"******** passivating journal")
      context.parent ! Passivate(stopMessage = Stop)

    case Stop =>
      log.info(s"******** stopping journal")
      context.stop(self)
  }

  def persistInCassandra(msg:Message):Unit = {
    log.info("******** saving delta in cassandra")
  }

}

