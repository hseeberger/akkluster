/*
 * Copyright 2018 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rocks.heikoseeberger.acuar

import akka.actor.{ ActorSystem, CoordinatedShutdown }
import akka.cluster.typed.{ Cluster, Subscribe }
import akka.cluster.ClusterEvent.{
  MemberEvent,
  MemberJoined,
  MemberUp,
  ReachabilityEvent,
  ReachableMember,
  UnreachableMember
}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Route
import akka.stream.{ Materializer, OverflowStrategy }
import akka.stream.typed.scaladsl.ActorSource
import akka.NotUsed
import akka.cluster.Member
import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success }

object Api {

  final case class Config(address: String,
                          port: Int,
                          clusterEventsBufferSize: Int,
                          clusterEventsKeepAlive: FiniteDuration)

  private final object BindFailure extends CoordinatedShutdown.Reason

  def apply(config: Config, cluster: Cluster)(implicit untypedSystem: ActorSystem,
                                              mat: Materializer): Unit = {
    import config._
    import untypedSystem.dispatcher

    val log      = Logging(untypedSystem, this.getClass.getName)
    val shutdown = CoordinatedShutdown(untypedSystem)

    Http()
      .bindAndHandle(route(config, cluster), address, port)
      .onComplete {
        case Failure(cause) =>
          log.error(cause, "Shutting down, because cannot bind to {}:{}!", address, port)
          shutdown.run(BindFailure)

        case Success(binding) =>
          log.info("Listening for HTTP connections on {}", binding.localAddress)
          shutdown.addTask(CoordinatedShutdown.PhaseServiceUnbind, "api.unbind") { () =>
            binding.unbind()
          }
      }
  }

  def route(config: Config, cluster: Cluster): Route = {
    import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
    import akka.http.scaladsl.server.Directives._
    import config._

    pathSingleSlash {
      get {
        complete {
          StatusCodes.OK
        }
      }
    } ~
    path("cluster" / "events") {
      get {
        complete {
          val memberEvents =
            subscribeToMemberEvents(clusterEventsBufferSize, cluster).map(toServerSentEvent)
          val reachabilityEvents =
            subscribeToReachabilityEvents(clusterEventsBufferSize, cluster).map(toServerSentEvent)

          val events = memberEvents.merge(reachabilityEvents, eagerComplete = true)

          events.keepAlive(clusterEventsKeepAlive, () => ServerSentEvent.heartbeat)
        }
      }
    }
  }

  private def subscribeToMemberEvents(bufferSize: Int, cluster: Cluster) =
    ActorSource
      .actorRef[MemberEvent](PartialFunction.empty,
                             PartialFunction.empty,
                             bufferSize,
                             OverflowStrategy.fail)
      .mapMaterializedValue { ref =>
        cluster.subscriptions ! Subscribe(ref, classOf[MemberEvent])
        NotUsed
      }

  private def subscribeToReachabilityEvents(bufferSize: Int, cluster: Cluster) =
    ActorSource
      .actorRef[ReachabilityEvent](PartialFunction.empty,
                                   PartialFunction.empty,
                                   bufferSize,
                                   OverflowStrategy.fail)
      .mapMaterializedValue { ref =>
        cluster.subscriptions ! Subscribe(ref, classOf[ReachabilityEvent])
        NotUsed
      }

  private def toServerSentEvent(event: MemberEvent) =
    event match {
      case MemberJoined(member) => ServerSentEvent(addr(member), "member-joined")
      case MemberUp(member)     => ServerSentEvent(addr(member), "member-up")
    }

  private def toServerSentEvent(event: ReachabilityEvent) =
    event match {
      case UnreachableMember(member) => ServerSentEvent(addr(member), "unreachable-member")
      case ReachableMember(member)   => ServerSentEvent(addr(member), "Reachable-member")
    }

  private def addr(member: Member) = member.uniqueAddress.address.toString
}
