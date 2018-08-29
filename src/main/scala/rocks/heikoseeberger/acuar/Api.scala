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

import akka.actor.{ ActorSystem, CoordinatedShutdown }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import org.apache.logging.log4j.scala.Logging
import scala.util.{ Failure, Success }

object Api extends Logging {

  final case class Config(address: String, port: Int)

  private final object BindFailure extends CoordinatedShutdown.Reason

  def apply(config: Config)(implicit untypedSystem: ActorSystem, mat: Materializer): Unit = {
    import config._
    import untypedSystem.dispatcher

    val shutdown = CoordinatedShutdown(untypedSystem)

    Http()
      .bindAndHandle(route, address, port)
      .onComplete {
        case Failure(cause) =>
          logger.error(s"Shutting down, because cannot bind to $address:$port!", cause)
          shutdown.run(BindFailure)

        case Success(binding) =>
          logger.info(s"Listening for HTTP connections on ${binding.localAddress}")
          shutdown.addTask(CoordinatedShutdown.PhaseServiceUnbind, "api.unbind") { () =>
            binding.unbind()
          }
      }
  }

  def route: Route = {
    import akka.http.scaladsl.server.Directives._

    pathSingleSlash {
      get {
        complete {
          StatusCodes.OK
        }
      }
    }
  }
}
