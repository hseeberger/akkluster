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

import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.cluster.ClusterEvent.{
  ClusterDomainEvent,
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
import akka.cluster.Member
import akka.cluster.typed.{ ClusterStateSubscription, Subscribe }
import scala.concurrent.duration.FiniteDuration
import scala.reflect.{ ClassTag, classTag }

object ClusterEvents {

  def apply(bufferSize: Int,
            keepAlive: FiniteDuration,
            subscriptions: ActorRef[ClusterStateSubscription]): Source[ServerSentEvent, NotUsed] = {
    val memberEvents       = subscribe[MemberEvent](bufferSize, subscriptions).map(toSse)
    val reachabilityEvents = subscribe[ReachabilityEvent](bufferSize, subscriptions).map(toSse)
    val events             = memberEvents.merge(reachabilityEvents, eagerComplete = true)
    events.keepAlive(keepAlive, () => ServerSentEvent.heartbeat)
  }

  private def subscribe[A <: ClusterDomainEvent: ClassTag](
      bufferSize: Int,
      subscriptions: ActorRef[ClusterStateSubscription]
  ) =
    ActorSource
      .actorRef[A](PartialFunction.empty, PartialFunction.empty, bufferSize, OverflowStrategy.fail)
      .mapMaterializedValue { ref =>
        subscriptions ! Subscribe(ref, classTag[A].runtimeClass.asInstanceOf[Class[A]])
        NotUsed
      }

  private def toSse(event: MemberEvent) =
    event match {
      case MemberJoined(member)     => ServerSentEvent(addr(member), "member-joined")
      case MemberWeaklyUp(member)   => ServerSentEvent(addr(member), "member-weakly-up")
      case MemberUp(member)         => ServerSentEvent(addr(member), "member-up")
      case MemberLeft(member)       => ServerSentEvent(addr(member), "member-left")
      case MemberExited(member)     => ServerSentEvent(addr(member), "member-exited")
      case MemberRemoved(member, _) => ServerSentEvent(addr(member), "member-removed")
    }

  private def toSse(event: ReachabilityEvent) =
    event match {
      case UnreachableMember(member) => ServerSentEvent(addr(member), "unreachable-member")
      case ReachableMember(member)   => ServerSentEvent(addr(member), "reachable-member")
    }

  private def addr(member: Member) = member.uniqueAddress.address.toString
}
