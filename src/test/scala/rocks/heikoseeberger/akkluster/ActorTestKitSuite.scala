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

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.stream.Materializer
import munit.FunSuite
import scala.concurrent.ExecutionContext

abstract class ActorTestKitSuite extends FunSuite {

  protected val testKit: Fixture[ActorTestKit] =
    new Fixture[ActorTestKit]("testkit") {
      private val testKit = ActorTestKit()

      override def apply(): ActorTestKit =
        testKit

      override def afterAll(): Unit = {
        testKit.shutdownTestKit()
        super.afterAll()
      }
    }

  protected implicit val system: ActorSystem[Nothing] =
    testKit().system

  protected implicit val mat: Materializer =
    Materializer(testKit().system)

  protected implicit val ec: ExecutionContext =
    testKit().system.executionContext
}
