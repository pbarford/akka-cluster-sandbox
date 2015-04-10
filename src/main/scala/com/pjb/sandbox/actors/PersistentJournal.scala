package com.pjb.sandbox.actors

import akka.actor.{Props, ReceiveTimeout, ActorLogging}
import akka.actor.SupervisorStrategy.Stop
import akka.persistence.{RecoveryCompleted, PersistentActor}
import akka.contrib.pattern.ShardRegion
import scala.concurrent.duration._

object PersistentJournal {

  val shardName = "persistentJournal"

  def props():Props = Props(new PersistentJournal)

  val idExtractor: ShardRegion.IdExtractor = {
    case msg: Message => (idFromMessageKey(msg.key), msg)
  }

  val shardResolver: ShardRegion.ShardResolver = msg => msg match {
    case m: Message => (math.abs(idFromMessageKey(m.key).hashCode) % 100).toString
  }
  private def idFromMessageKey(key:MessageKey):String = {
    key match {
      case (Some(dest), eventId) => s"$dest:$eventId"
      case (None, eventId) => eventId.toString
    }
  }
}

class PersistentJournal extends PersistentActor with ActorLogging {

  import akka.contrib.pattern.ShardRegion.Passivate
  context.setReceiveTimeout(1.minutes)

  var state:Option[EventState] = None
  val creationTime = System.currentTimeMillis()

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  override def receiveRecover: Receive = {
    case restoredState:EventState =>
      log.info(s"******** restoring state upto seqNo : ${restoredState.uptoSeqNo}")
      state = Some(restoredState)
    case RecoveryCompleted =>
      if(state.isDefined) {
        log.info(s"******** state upto seqNo : ${state.get.uptoSeqNo}")
      } else {
        log.info(s"******** no state recovered")
      }
      log.info(s"******** journal started for $persistenceId in ${System.currentTimeMillis() - creationTime}ms")
  }

  override def receiveCommand: Receive = {
    case msg:Message =>
      log.info(s"******** msg received ${msg.data} ")
      persist(EventState(msg.key, msg.seqNo, msg.data)) { merged =>
        state = Some(merged)
        persistInCassandra(msg)
      }
      sender() ! AckMsg(msg.deliveryInfo)

    case ReceiveTimeout =>
      log.info(s"******** passivating journal for $persistenceId")
      context.parent ! Passivate(stopMessage = Stop)

    case Stop =>
      log.info(s"******** stopping journal for $persistenceId")
      context.stop(self)
  }

  def persistInCassandra(msg:Message):Unit = {
    log.info("******** saving delta in cassandra")
  }

}
