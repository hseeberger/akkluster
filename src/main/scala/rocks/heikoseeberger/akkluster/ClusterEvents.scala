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
import akka.actor.Address
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
import scala.concurrent.duration.FiniteDuration
import scala.reflect.{ ClassTag, classTag }

object ClusterEvents {

  final case class Config(bufferSize: Int, keepAlive: FiniteDuration)

  private final case class ClusterEvent(address: Address, status: String, label: String = "")

  private def nextLabel(label: String) =
    label.last.toUpper match {
      case 'Z' => label + "A"
      case c   => label.init + (c + 1).toChar
    }

  def apply(config: Config,
            cluster: ActorRef[ClusterStateSubscription]): Source[ServerSentEvent, NotUsed] = {
    import config._
    import io.circe.syntax._
    import io.circe.generic.auto._

    val memberEvents       = subscribe[MemberEvent](bufferSize, cluster).map(toClusterEvent)
    val reachabilityEvents = subscribe[ReachabilityEvent](bufferSize, cluster).map(toClusterEvent)
    val events             = memberEvents.merge(reachabilityEvents, eagerComplete = true)

    var labels = Map.empty[Address, String]
    events
      .statefulMapConcat(() => {
        case event @ ClusterEvent(address, status, _) =>
          val label =
            labels.get(address) match {
              case None =>
                val label = if (labels.isEmpty) "A" else nextLabel(labels.values.max)
                labels += address -> label
                label
              case Some(label) =>
                if (status == "removed") labels -= address
                label
            }
          List(event.copy(label = label))
      })
      .map(e => ServerSentEvent(e.asJson.noSpaces))
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
      case MemberJoined(member)     => ClusterEvent(member.address, "joining")
      case MemberWeaklyUp(member)   => ClusterEvent(member.address, "weakly-up")
      case MemberUp(member)         => ClusterEvent(member.address, "up")
      case MemberLeft(member)       => ClusterEvent(member.address, "leaving")
      case MemberExited(member)     => ClusterEvent(member.address, "exiting")
      case MemberDowned(member)     => ClusterEvent(member.address, "down")
      case MemberRemoved(member, _) => ClusterEvent(member.address, "removed")
    }

  private def toClusterEvent(event: ReachabilityEvent) =
    event match {
      case UnreachableMember(member) => ClusterEvent(member.address, "unreachable")
      case ReachableMember(member)   => ClusterEvent(member.address, "reachable")
    }
}
