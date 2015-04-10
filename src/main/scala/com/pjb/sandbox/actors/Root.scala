package com.pjb.sandbox.actors

import akka.actor.{Props, Actor}
import com.pjb.sandbox.modules.NamedActorFactory

object Root {
  def props(consumerFactory:NamedActorFactory):Props =
    Props(new Root(consumerFactory))
}

class Root(consumerFactory:NamedActorFactory) extends Actor {

  consumerFactory(context, "testQ")

  override def receive: Receive = {
    case _ =>
  }
}
