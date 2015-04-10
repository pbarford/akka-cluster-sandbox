package com.pjb.sandbox

import akka.actor.{ActorRef, ActorRefFactory}

package object modules {
  type ActorFactory = ActorRefFactory => ActorRef
  type NamedActorFactory = (ActorRefFactory, String) => ActorRef
}
