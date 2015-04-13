package com.pjb.sandbox.web

import javax.servlet.ServletContext

import com.pjb.sandbox.http.LatestState
import com.pjb.sandbox.modules.{CassandraModule, RabbitMqModule, ConfigModule, ActorModule}
import spray.servlet.WebBoot

class Boot(servletContext:ServletContext) extends WebBoot {

  val modules = new ActorModule with RabbitMqModule with CassandraModule with ConfigModule

  implicit val system = modules.actorSystem
  val serviceActor = modules.rootActor()
}
