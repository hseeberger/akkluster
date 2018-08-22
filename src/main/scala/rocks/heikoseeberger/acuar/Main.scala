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

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ ActorSystem, Behavior }
import akka.cluster.typed.{ Cluster, SelfUp, Subscribe }
import akka.management.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
import org.apache.logging.log4j.scala.Logging
import pureconfig.loadConfigOrThrow

object Main extends Logging {

  private final case class Config()

  def main(args: Array[String]): Unit = {
    sys.props += "log4j2.contextSelector" -> classOf[AsyncLoggerContextSelector].getName // Always use async logging!

    val config = loadConfigOrThrow[Config]("acuar") // Must be first to aviod creating the actor system on failure!
    val system = ActorSystem(Main(config), "acuar")

    AkkaManagement(system.toUntyped).start()
    ClusterBootstrap(system.toUntyped).start()
  }

  def apply(config: Config): Behavior[SelfUp] =
    Behaviors.setup { context =>
      context.log.info("{} started and ready to join cluster", context.system.name)
      Cluster(context.system).subscriptions ! Subscribe(context.self, classOf[SelfUp])

      Behaviors
        .receive[SelfUp] { (context, _) =>
          context.log.info("{} joined cluster and is up", context.system.name)
          Behaviors.empty
        }
    }
}
