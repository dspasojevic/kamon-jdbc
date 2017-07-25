/* =========================================================================================
 * Copyright © 2013-2014 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.jdbc.instrumentation

import java.lang.reflect.Method
import java.sql.Driver

import kamon.Kamon
import kamon.agent.libs.net.bytebuddy.asm.Advice.{AllArguments, Enter, OnMethodEnter, OnMethodExit, Origin, This, Thrown}
import kamon.agent.scala.KamonInstrumentation
import kamon.jdbc.mixin.{DriverAware, DriverMixin}
import kamon.trace.{ActiveSpan, Span}

class DriverInstrumentation extends KamonInstrumentation {

  import kamon.agent.libs.net.bytebuddy.matcher.{ElementMatchers => BBMatchers}

  forSubtypeOf("java.sql.Driver") { builder ⇒
    builder
      .withMixin(classOf[DriverMixin])
      .withAdvisorFor(BBMatchers.nameStartsWith("connect"),
                      classOf[DriverAdvisor])
      .build()
  }
}

class DriverAdvisor

object DriverAdvisor {

  @OnMethodEnter
  def onEnter(@This driverMixin: DriverAware,
              @This driver: Driver,
              @Origin method: Method,
              @AllArguments args: Array[Object]): ActiveSpan = {

    val span = Kamon.buildSpan("connect")

    if (args.length > 0) span.withSpanTag("url", args(0).toString)

    span.withSpanTag("protocol", "jdbc")
    span.withSpanTag("driverMajorVersion", driver.getMajorVersion)
    span.withSpanTag("driverMinorVersion", driver.getMinorVersion)

    val started: Span = span.start()
    val activeSpan = Kamon.makeActive(started)

    activeSpan
  }

  @OnMethodExit(onThrowable = classOf[Throwable])
  def onExit(@This driverMixin: DriverAware,
             @Origin method: Method,
             @Thrown throwable: Throwable,
             @Enter activeSpan: ActiveSpan): Unit = {
    if (throwable != null) {
      activeSpan.addSpanTag("error", value = true)
      activeSpan.addSpanTag("throwable", throwable.toString)
    }

    activeSpan.deactivate()
    activeSpan.finish()
  }

  object DriverTypes {
    val Query = "query"
    val Update = "update"
    val Batch = "batch"
    val GenericExecute = "generic-execute"
  }

}
