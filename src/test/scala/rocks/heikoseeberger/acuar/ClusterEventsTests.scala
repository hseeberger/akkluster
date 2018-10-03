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

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.testkit.typed.FishingOutcome
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.Address
import akka.cluster.{ Member, MemberStatus, UniqueAddress }
import akka.cluster.typed.{ ClusterStateSubscription, Subscribe }
import akka.cluster.ClusterEvent.{
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
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Sink, Source }
import org.mockito.Mockito
import scala.concurrent.duration.DurationInt
import utest._

object ClusterEventsTests extends ActorTestSuite {
  import testkit._

  override def tests: Tests =
    Tests {
      'subscribe - {
        val subscriptions = TestProbe[ClusterStateSubscription]()
        ClusterEvents(42, 42.seconds, subscriptions.ref).runWith(Sink.ignore)
        subscriptions.fishForMessage(3.seconds) {
          case Subscribe(_, c) if c == classOf[MemberEvent]       => FishingOutcome.Continue
          case Subscribe(_, c) if c == classOf[ReachabilityEvent] => FishingOutcome.Complete
          case other                                              => FishingOutcome.Fail(s"Unexpected message: $other")
        }
      }

      'events - {
        val (memberEventsQueue, memberEvents) =
          Source.queue[MemberEvent](1, OverflowStrategy.fail).preMaterialize()
        val (reachabilityEventsQueue, reachabilityEvents) =
          Source.queue[ReachabilityEvent](1, OverflowStrategy.fail).preMaterialize()
        val subscriptions =
          spawn(Behaviors.receiveMessage[ClusterStateSubscription] {
            case Subscribe(s: ActorRef[MemberEvent], c) if c == classOf[MemberEvent] =>
              memberEvents.runForeach(s.!)
              Behaviors.same

            case Subscribe(s: ActorRef[ReachabilityEvent], c) if c == classOf[ReachabilityEvent] =>
              reachabilityEvents.runForeach(s.!)
              Behaviors.same
          })
        val events = ClusterEvents(42, 42.seconds, subscriptions).take(8).runWith(Sink.seq)

        val member = Mockito.mock(classOf[Member])
        Mockito.when(member.uniqueAddress).thenReturn(UniqueAddress(Address("akka", "test"), 1L))
        Mockito.when(member.status).thenReturn(MemberStatus.Joining)

        val expected =
          List(
            ServerSentEvent(member.uniqueAddress.address.toString, "member-joined"),
            ServerSentEvent(member.uniqueAddress.address.toString, "member-weakly-up"),
            ServerSentEvent(member.uniqueAddress.address.toString, "member-up"),
            ServerSentEvent(member.uniqueAddress.address.toString, "unreachable-member"),
            ServerSentEvent(member.uniqueAddress.address.toString, "reachable-member"),
            ServerSentEvent(member.uniqueAddress.address.toString, "member-left"),
            ServerSentEvent(member.uniqueAddress.address.toString, "member-exited"),
            ServerSentEvent(member.uniqueAddress.address.toString, "member-removed")
          )

        for {
          _ <- memberEventsQueue.offer(MemberJoined(member))
          _ = Mockito.when(member.status).thenReturn(MemberStatus.WeaklyUp)
          _ <- memberEventsQueue.offer(MemberWeaklyUp(member))
          _ = Mockito.when(member.status).thenReturn(MemberStatus.Up)
          _ <- memberEventsQueue.offer(MemberUp(member))
          _ <- reachabilityEventsQueue.offer(UnreachableMember(member))
          _ <- reachabilityEventsQueue.offer(ReachableMember(member))
          _ = Mockito.when(member.status).thenReturn(MemberStatus.Leaving)
          _ <- memberEventsQueue.offer(MemberLeft(member))
          _ = Mockito.when(member.status).thenReturn(MemberStatus.Exiting)
          _ <- memberEventsQueue.offer(MemberExited(member))
          _ = Mockito.when(member.status).thenReturn(MemberStatus.Removed)
          _  <- memberEventsQueue.offer(MemberRemoved(member, MemberStatus.Exiting))
          es <- events
        } yield assert(es == expected)
      }
    }

  override def utestAfterAll(): Unit = {
    shutdownTestKit()
    super.utestAfterAll()
  }
}
