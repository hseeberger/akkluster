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

import akka.actor.{ ActorSystem => UntypedSystem }
import akka.actor.typed.{ ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.cluster.typed.{ Cluster, SelfUp, Subscribe, Unsubscribe }
import akka.management.scaladsl.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.stream.Materializer
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
import pureconfig.generic.auto.exportReader
import pureconfig.loadConfigOrThrow

object Main {

  private final case class Config(api: Api.Config)

  def main(args: Array[String]): Unit = {
    sys.props += "log4j2.contextSelector" -> classOf[AsyncLoggerContextSelector].getName // Always use async logging!

    val config = loadConfigOrThrow[Config]("akkluster") // Must be first to aviod creating the actor system on failure!
    val system = ActorSystem(Main(config), "akkluster")

    AkkaManagement(system.toUntyped).start()
    ClusterBootstrap(system.toUntyped).start()
  }

  def apply(config: Config): Behavior[SelfUp] =
    Behaviors.setup { context =>
      context.log.info("{} started and ready to join cluster", context.system.name)

      val cluster = Cluster(context.system)
      cluster.subscriptions ! Subscribe(context.self, classOf[SelfUp])

      Behaviors.receive { (context, _) =>
        context.log.info("{} joined cluster and is up", context.system.name)

        cluster.subscriptions ! Unsubscribe(context.self)

        implicit val untypedSystem: UntypedSystem = context.system.toUntyped
        implicit val mat: Materializer            = ActorMaterializer()(context.system)

        Api(config.api, cluster.subscriptions)

        Behaviors.empty
      }
    }
}
