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

package rocks.heikoseeberger.akkluster

import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.cluster.ClusterEvent.{
  ClusterDomainEvent,
  MemberDowned,
  MemberEvent,
  MemberExited,
  MemberJoined,
  MemberLeft,
  MemberRemoved,
  MemberUp,
  MemberWeaklyUp,
  ReachabilityEvent,
  ReachableMember,
  UnreachableMember
}
import akka.cluster.typed.{ ClusterStateSubscription, Subscribe }
import akka.cluster.Member
import io.bullet.borer.{ Codec, Json }
import scala.concurrent.duration.FiniteDuration
import scala.reflect.{ ClassTag, classTag }

object ClusterEvents {

  final case class Config(bufferSize: Int, keepAlive: FiniteDuration)

  private final object ClusterEvent {
    import io.bullet.borer.derivation.MapBasedCodecs._

    implicit val codec: Codec[ClusterEvent] = deriveCodec

    def apply(member: Member, status: String): ClusterEvent =
      ClusterEvent(member.address.toString, status, member.roles)
  }

  private final case class ClusterEvent(address: String, status: String, roles: Set[String])

  def apply(
      config: Config,
      cluster: ActorRef[ClusterStateSubscription]
  ): Source[ServerSentEvent, NotUsed] = {
    import config._

    val memberEvents       = subscribe[MemberEvent](bufferSize, cluster).map(toClusterEvent)
    val reachabilityEvents = subscribe[ReachabilityEvent](bufferSize, cluster).map(toClusterEvent)
    val events             = memberEvents.merge(reachabilityEvents, eagerComplete = true)

    events
      .map(event => ServerSentEvent(Json.encode(event).toUtf8String))
      .keepAlive(keepAlive, () => ServerSentEvent.heartbeat)
  }

  private def subscribe[A <: ClusterDomainEvent: ClassTag](
      bufferSize: Int,
      cluster: ActorRef[ClusterStateSubscription]
  ) =
    ActorSource
      .actorRef[A](PartialFunction.empty, PartialFunction.empty, bufferSize, OverflowStrategy.fail)
      .mapMaterializedValue { ref =>
        cluster ! Subscribe(ref, classTag[A].runtimeClass.asInstanceOf[Class[A]])
        NotUsed
      }

  private def toClusterEvent(event: MemberEvent) =
    event match {
      case MemberJoined(m)     => ClusterEvent(m, "joining")
      case MemberWeaklyUp(m)   => ClusterEvent(m, "weakly-up")
      case MemberUp(m)         => ClusterEvent(m, "up")
      case MemberLeft(m)       => ClusterEvent(m, "leaving")
      case MemberExited(m)     => ClusterEvent(m, "exiting")
      case MemberDowned(m)     => ClusterEvent(m, "down")
      case MemberRemoved(m, _) => ClusterEvent(m, "removed")
    }

  private def toClusterEvent(event: ReachabilityEvent) =
    event match {
      case UnreachableMember(m) => ClusterEvent(m, "unreachable")
      case ReachableMember(m)   => ClusterEvent(m, "reachable")
    }
}
