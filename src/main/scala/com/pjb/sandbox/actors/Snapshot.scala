package com.pjb.sandbox.actors

import akka.actor.{ActorLogging, Props, Actor}
import com.datastax.driver.core.Session

object Snapshot {
  def props(cassandraSession:() => Session):Props = Props(new Snapshot(cassandraSession))
}

class Snapshot(cassandraSession:() => Session) extends Actor with ActorLogging {
  var state:Option[EventState] = Some(EventState((Some("blah"), 12345), 1, "init"))

  val session = cassandraSession()
  val key:Option[CorrelationKey] = getKeyFromParentName()

  def getKeyFromParentName():Option[CorrelationKey] = {
    self.path.parent.name.split('.') match {
      case Array(dest:String, eventId:String) =>
        println(s"$dest--->$eventId")
        Some((Some(dest),Long.unbox(eventId)))
      case Array(eventId:String) =>
        println(s"$eventId")
        Some((None,Long.unbox(eventId)))
      case _ =>
        None
    }
  }

  override def postStop(): Unit = {
    log.info("Snapshot saying bye bye")
  }

  override def receive: Receive = {

    case msg:Message =>
      log.info(s"******** msg received ${msg.data} ")
      state = Some(EventState(msg.key, msg.seqNo, msg.data))

    case GetLatestState =>
      log.info("Get Latest State!!!")
      sender() ! state.get
  }
}
