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

import akka.actor.{ ActorSystem, CoordinatedShutdown }
import akka.actor.typed.ActorRef
import akka.cluster.typed.ClusterStateSubscription
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ StatusCodes, Uri }
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.Done
import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success }

object Api {

  final case class Config(address: String,
                          port: Int,
                          terminationDeadline: FiniteDuration,
                          clusterEvents: ClusterEvents.Config)

  private final object BindFailure extends CoordinatedShutdown.Reason

  def apply(
      config: Config,
      subscriptions: ActorRef[ClusterStateSubscription]
  )(implicit untypedSystem: ActorSystem, mat: Materializer): Unit = {
    import config._
    import untypedSystem.dispatcher

    val log      = Logging(untypedSystem, this.getClass.getName)
    val shutdown = CoordinatedShutdown(untypedSystem)

    Http()
      .bindAndHandle(route(config, subscriptions), address, port)
      .onComplete {
        case Failure(cause) =>
          log.error(cause, "Shutting down, because cannot bind to {}:{}!", address, port)
          shutdown.run(BindFailure)

        case Success(binding) =>
          log.info("Listening for HTTP connections on {}", binding.localAddress)
          shutdown.addTask(CoordinatedShutdown.PhaseServiceUnbind, "api.unbind") { () =>
            binding.terminate(terminationDeadline).map(_ => Done)
          }
      }
  }

  def route(config: Config, subscriptions: ActorRef[ClusterStateSubscription]): Route = {
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
          ClusterEvents(config.clusterEvents, subscriptions)
        }
      }
    }
  }
}
