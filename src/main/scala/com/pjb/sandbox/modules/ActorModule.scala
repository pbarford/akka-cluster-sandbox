package com.pjb.sandbox.modules

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.contrib.pattern.{ClusterSharding, ClusterSingletonManager}
import com.pjb.sandbox.actors._

trait ActorModule {

  this: RabbitMqModule with CassandraModule with ConfigModule  =>

  lazy val actorSystem:ActorSystem = {
    val system = ActorSystem("ClusterSystem", configuration)

    ClusterSharding(system).start(
      typeName = Journal.shardName,
      entryProps = Some(Journal.props(cassandraSession)),
      idExtractor = Journal.idExtractor,
      shardResolver = Journal.shardResolver)

    system
  }

  def consumerFactory:NamedActorFactory = {
    (ctx, name) => ctx.actorOf(ClusterSingletonManager.props(
      singletonProps = MessageConsumer.props(rabbitConnectionFactory, name),
      singletonName = s"messageConsumer_$name",
      terminationMessage = Stop,
      role = Some("sandbox")),
      name = "singleton")
  }

  def rootActor():ActorRef = {
    actorSystem.actorOf(Root.props(consumerFactory), "root")
  }
}
