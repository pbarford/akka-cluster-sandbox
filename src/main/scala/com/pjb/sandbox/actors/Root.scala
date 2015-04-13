package com.pjb.sandbox.actors

import akka.actor.{ActorRefFactory, Props, Actor}
import com.pjb.sandbox.http.LatestState
import com.pjb.sandbox.modules.NamedActorFactory
import spray.routing.HttpService

object Root {
  def props(consumerFactory:NamedActorFactory):Props =
    Props(new Root(consumerFactory))
}

class Root(consumerFactory:NamedActorFactory) extends Actor with HttpService {
  override val actorRefFactory: ActorRefFactory = context
  val latestState = new LatestState(context.system)
  consumerFactory(context, "testQ")
  override def receive: Receive = runRoute(latestState.routes)
}
