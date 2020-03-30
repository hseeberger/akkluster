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

import akka.actor.{ ActorSystem => ClassicSystem }
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.{ ClassicActorSystemOps, TypedActorSystemOps }
import akka.cluster.typed.{ Cluster, SelfUp, Subscribe, Unsubscribe }
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
import pureconfig.generic.auto.exportReader
import pureconfig.ConfigSource

object Main {

  private final val Name = "akkluster"

  private final case class Config(httpServer: HttpServer.Config)

  def main(args: Array[String]): Unit = {
    // Always use async logging!
    sys.props += "log4j2.contextSelector" -> classOf[AsyncLoggerContextSelector].getName

    // Happens before creating the actor system to fail fast
    val config = ConfigSource.default.at(Name).loadOrThrow[Config]

    // Use a classic system, because some libraries still rely on it
    val classicSystem = ClassicSystem(Name)
    AkkaManagement(classicSystem).start()
    ClusterBootstrap(classicSystem).start()
    classicSystem.spawn(Main(config), "main")
  }

  def apply(config: Config): Behavior[SelfUp] =
    Behaviors.setup { context =>
      import context.log

      val cluster = Cluster(context.system)

      if (log.isInfoEnabled)
        log.info(s"${context.system.name} started and ready to join cluster")
      cluster.subscriptions ! Subscribe(context.self, classOf[SelfUp])

      Behaviors.receive { (context, _) =>
        if (log.isInfoEnabled)
          log.info(s"${context.system.name} joined cluster and is up")
        cluster.subscriptions ! Unsubscribe(context.self)

        implicit val classicSystem: ClassicSystem = context.system.toClassic

        HttpServer.run(config.httpServer, cluster.subscriptions)

        Behaviors.empty
      }
    }
}
