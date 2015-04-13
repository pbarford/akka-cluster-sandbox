package com.pjb.sandbox.http

import java.util.concurrent.Executors

import akka.actor.{ActorRef, ActorSystem, ActorRefFactory}
import akka.contrib.pattern.ClusterSharding
import akka.event.slf4j.SLF4JLogging
import akka.pattern.ask
import akka.util.Timeout
import com.pjb.sandbox.actors.{EventState, GetLatestState, Journal}
import spray.http.MediaTypes._
import spray.routing.HttpService

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

class LatestState(actorSystem: ActorSystem) extends HttpService with SLF4JLogging {
  override implicit def actorRefFactory: ActorRefFactory = actorSystem

  implicit val timeout: Timeout = 10.seconds
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10)) //scalastyle:ignore

  val journalRegion:ActorRef = ClusterSharding(actorSystem).shardRegion(Journal.shardName)

  val routes = {
    pathPrefix("lateststate") {
      path("destination" / Segment / LongNumber) { (dest, eventId) =>
        get {
          respondWithMediaType(`application/json`) {
            val receivedAt = System.currentTimeMillis()
            log.info(s"looking up $dest-$eventId")
            onComplete(journalRegion ? GetLatestState((Some(dest), eventId))) {
              case Success(latestState:EventState) =>
                log.info(s"latestState for dest=$dest, eventId=$eventId, duration=${System.currentTimeMillis() - receivedAt}") //scalastyle:ignore
                complete(latestState.data)
              case Success(invalidResponse) => failWith(new IllegalStateException(s"unexpected response - $invalidResponse"))
              case Failure(ex) => failWith(ex)
            }
          }
        }
      }
    }
  }
}
