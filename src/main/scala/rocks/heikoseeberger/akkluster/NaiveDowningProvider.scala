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

import akka.actor.{ Actor, ActorSystem, Address, Props, Timers }
import akka.cluster.{ Cluster, ClusterEvent, DowningProvider }
import akka.cluster.ClusterEvent.{ ReachabilityEvent, ReachableMember, UnreachableMember }
import rocks.heikoseeberger.akkluster.NaiveDowning.StableUnreachable
import scala.concurrent.duration.{ Duration, DurationInt, FiniteDuration }

/**
  * Attention: Just for demo purposes, don't use this in production!
  */
final class NaiveDowningProvider(system: ActorSystem) extends DowningProvider {

  override def downRemovalMargin: FiniteDuration =
    Duration.Zero

  override def downingActorProps: Option[Props] =
    Some(Props(new NaiveDowning(7.seconds)))
}

/**
  * Attention: Just for demo purposes, don't use this in production!
  */
object NaiveDowning {
  private final case class StableUnreachable(address: Address)
}

/**
  * Attention: Just for demo purposes, don't use this in production!
  */
final class NaiveDowning(stableMargin: FiniteDuration) extends Actor with Timers {

  private val cluster = Cluster(context.system)

  cluster.subscribe(context.self, ClusterEvent.InitialStateAsEvents, classOf[ReachabilityEvent])

  override def receive: Receive = {
    case UnreachableMember(member) =>
      val stableUnreachable = StableUnreachable(member.address)
      timers.startSingleTimer(stableUnreachable, stableUnreachable, stableMargin)

    case ReachableMember(member) =>
      timers.cancel(StableUnreachable(member.address))

    case StableUnreachable(address) =>
      cluster.down(address)
  }
}
