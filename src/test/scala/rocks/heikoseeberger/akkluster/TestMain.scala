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
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ HttpApp, Route }
import akka.stream.scaladsl.Source
import akka.stream.ThrottleMode
import scala.concurrent.duration.DurationInt

object TestMain extends HttpApp {

  def main(args: Array[String]): Unit =
    startServer("0.0.0.0", 8080)

  override protected def routes: Route =
    pathSingleSlash {
      get {
        redirect("index.html", StatusCodes.PermanentRedirect)
      }
    } ~
    getFromDirectory("src/main/resources/web") ~
    path("events") {
      import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
      get {
        complete {
          val initial =
            List(
              ServerSentEvent("akka://192.168.99.100:25520/akkluster", "up")
            )
          val ongoing =
            List(
              ServerSentEvent("akka://192.168.99.101:25520/akkluster", "message"),
              ServerSentEvent("akka://192.168.99.102:25520/akkluster"),
              ServerSentEvent("akka://192.168.99.102:25520/akkluster", "unreachable"),
              ServerSentEvent("akka://192.168.99.101:25520/akkluster", "left"),
              ServerSentEvent("akka://192.168.99.101:25520/akkluster", "exited"),
              ServerSentEvent("akka://192.168.99.101:25520/akkluster", "removed"),
              ServerSentEvent("akka://192.168.99.102:25520/akkluster", "reachable")
            )

          Source(initial) ++ Source(ongoing).throttle(1, 2.seconds, 0, ThrottleMode.shaping) ++ Source.maybe
        }
      }
    }
}
