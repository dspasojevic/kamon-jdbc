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

import java.sql.Statement

import kamon.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodExit, Return}
import kamon.agent.scala.KamonInstrumentation
import kamon.jdbc.mixin.{StatementAware, StatementMixin}

class ConnectionInstrumentation extends KamonInstrumentation {

  import kamon.agent.libs.net.bytebuddy.matcher.{ElementMatchers => BBMatchers}

  println("Instrumenting connection")

  forSubtypeOf("java.sql.Connection") { builder ⇒
    builder
      .withAdvisorFor(BBMatchers.named("prepareStatement"),
        classOf[ConnectionAdvisor])
      .build()
  }
}

class ConnectionAdvisor

object ConnectionAdvisor {

  @OnMethodExit(onThrowable = classOf[Throwable])
  def onExit(@Return statement: AnyRef, @Argument(0) sql: String): Unit = {
    if (statement != null) {
      statement match {
        case aware: StatementAware =>
          println(s"Setting SQL to $sql")
          aware.setSql(Some(sql))
        case _ =>
          println(s"[${statement.getClass}] is not [${statement.isInstanceOf[StatementAware]}] [${statement.getClass.isAssignableFrom(classOf[Statement])}]")
          ()
      }
    }
    else {
      println(s"statement [$statement] is null")
    }
  }

}
