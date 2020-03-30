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

import akka.actor.{ CoordinatedShutdown, ActorSystem => ClassicSystem }
import akka.actor.typed.ActorRef
import akka.cluster.typed.ClusterStateSubscription
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ StatusCodes, Uri }
import akka.http.scaladsl.server.Route
import akka.Done
import org.slf4j.LoggerFactory
import scala.concurrent.{ Future, Promise }
import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success }

object HttpServer {

  final case class Config(
      interface: String,
      port: Int,
      terminationDeadline: FiniteDuration,
      clusterEvents: ClusterEvents.Config
  )

  final class ReadinessCheck extends (() => Future[Boolean]) {
    override def apply(): Future[Boolean] =
      ready.future
  }

  private final object BindFailure extends CoordinatedShutdown.Reason

  private val ready = Promise[Boolean]()

  def run(
      config: Config,
      cluster: ActorRef[ClusterStateSubscription]
  )(implicit classicSystem: ClassicSystem): Unit = {
    import classicSystem.dispatcher
    import config._

    val log      = LoggerFactory.getLogger(this.getClass)
    val shutdown = CoordinatedShutdown(classicSystem)

    Http()
      .bindAndHandle(route(config, cluster), interface, port)
      .onComplete {
        case Failure(cause) =>
          log.error(s"Shutting down, because cannot bind to $interface:$port!", cause)
          shutdown.run(BindFailure)

        case Success(binding) =>
          if (log.isInfoEnabled)
            log.info(s"Listening for HTTP connections on ${binding.localAddress}")
          ready.success(true)
          shutdown.addTask(CoordinatedShutdown.PhaseServiceUnbind, "terminate-http-server") { () =>
            binding.terminate(terminationDeadline).map(_ => Done)
          }
      }
  }

  def route(config: Config, cluster: ActorRef[ClusterStateSubscription]): Route = {
    import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
    import akka.http.scaladsl.server.Directives._

    pathSingleSlash {
      get {
        redirect(Uri("/index.html"), StatusCodes.PermanentRedirect)
      }
    } ~
    getFromResourceDirectory("web") ~
    path("events") {
      get {
        complete {
          ClusterEvents(config.clusterEvents, cluster)
        }
      }
    }
  }
}
