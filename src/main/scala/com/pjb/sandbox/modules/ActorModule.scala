package com.pjb.sandbox.modules

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.contrib.pattern.{ClusterSharding, ClusterSingletonManager}
import com.pjb.sandbox.actors._

trait ActorModule {

  this: RabbitMqModule with ConfigModule =>

  lazy val actorSystem:ActorSystem = {
    val system = ActorSystem("ClusterSystem", configuration)
/*
    ClusterSharding(system).start(
      typeName = LatestState.shardName,
      entryProps = Some(LatestState.props()),
      idExtractor = LatestState.idExtractor,
      shardResolver = LatestState.shardResolver)

    ClusterSharding(system).start(
      typeName = Journal.shardName,
      entryProps = Some(Journal.props()),
      idExtractor = Journal.idExtractor,
      shardResolver = Journal.shardResolver)
*/
    ClusterSharding(system).start(
      typeName = PersistentJournal.shardName,
      entryProps = Some(PersistentJournal.props()),
      idExtractor = PersistentJournal.idExtractor,
      shardResolver = PersistentJournal.shardResolver)

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
