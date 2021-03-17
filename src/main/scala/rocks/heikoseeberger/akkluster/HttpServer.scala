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

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ StatusCodes, Uri }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.NotUsed
import akka.stream.scaladsl.Source
import org.slf4j.LoggerFactory
import scala.concurrent.{ Future, Promise }
import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success }

object HttpServer {

  final case class Config(interface: String, port: Int, terminationDeadline: FiniteDuration)

  final class ReadinessCheck extends (() => Future[Boolean]) {
    override def apply(): Future[Boolean] =
      ready.future
  }

  private final object BindFailure extends CoordinatedShutdown.Reason

  private val ready = Promise[Boolean]()

  def run(config: Config, clusterEvents: Source[ServerSentEvent, NotUsed])(implicit
      system: ActorSystem[_]
  ): Unit = {
    import config._
    import system.executionContext

    val log      = LoggerFactory.getLogger(this.getClass)
    val shutdown = CoordinatedShutdown(system)

    Http()
      .newServerAt(interface, port)
      .bind(route(config, clusterEvents))
      .onComplete {
        case Failure(cause) =>
          log.error(s"Shutting down, because cannot bind to [$interface:$port]!", cause)
          shutdown.run(BindFailure)

        case Success(binding @ ServerBinding(address)) =>
          if (log.isInfoEnabled)
            log.info(s"Listening to HTTP connections on [$address]")
          ready.success(true)
          binding.addToCoordinatedShutdown(terminationDeadline)
      }
  }

  def route(config: Config, clusterEvents: Source[ServerSentEvent, NotUsed]): Route = {
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
          clusterEvents
        }
      }
    }
  }
}
