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
import java.sql.Statement

import kamon.Kamon
import kamon.agent.libs.net.bytebuddy.asm.Advice.{AllArguments, Enter, OnMethodEnter, OnMethodExit, Origin, This, Thrown}
import kamon.agent.scala.KamonInstrumentation
import kamon.jdbc.JdbcExtension
import kamon.jdbc.mixin.{StatementAware, StatementMixin}
import kamon.trace.{ActiveSpan, Span}

class StatementInstrumentation extends KamonInstrumentation {

  import kamon.agent.libs.net.bytebuddy.matcher.{ElementMatchers => BBMatchers}

  forSubtypeOf("java.sql.Statement") { builder ⇒
    builder
      .withMixin(classOf[StatementMixin])
      .withAdvisorFor(BBMatchers.nameStartsWith("execute"),
                      classOf[StatementAdvisor])
      .build()
  }
}

class StatementAdvisor

object StatementAdvisor {

  @OnMethodEnter
  def onEnter(@This statementMixin: StatementAware,
              @This statement: Statement,
              @Origin method: Method,
              @AllArguments args: Array[Object]): ActiveSpan = {

    println(statementMixin)
    println(statementMixin.getClass)
    println(statementMixin.sql)

    val sql = if (args.length > 0) Option(args(0)).map(_.toString) else Option(statementMixin.sql).flatten

    val statementType = method.getName match {
      case "executeUpdate" => StatementTypes.Update
      case "executeQuery"  => StatementTypes.Query
      case "executeBatch"  => StatementTypes.Batch
      case _               => StatementTypes.GenericExecute
    }

    statementMixin.inFlightStatements.increment()

    val segmentName =
      JdbcExtension.generateJdbcSegmentName(statementType, sql.getOrElse(""))

    val span = Kamon.buildSpan(segmentName)

    sql.foreach(s => span.withSpanTag("sql", s))
    span.withSpanTag("statementType", statementType)
    span.withSpanTag("autoCommit", statement.getConnection.getAutoCommit)
    span.withSpanTag("driverName", statement.getConnection.getMetaData.getDriverName)
    span.withSpanTag("userName", statement.getConnection.getMetaData.getUserName)
    span.withSpanTag("url", statement.getConnection.getMetaData.getURL)
    span.withSpanTag("driverVersion", statement.getConnection.getMetaData.getDriverVersion)
    span.withSpanTag("databaseProduct", statement.getConnection.getMetaData.getDatabaseProductName)
    span.withSpanTag("databaseVersion", statement.getConnection.getMetaData.getDatabaseProductVersion)
    span.withSpanTag("protocol", "jdbc")

    println(s"Span [$span].")

    val started: Span = span.start()

    println(s"Started [$started].")

    val activeSpan = Kamon.makeActive(started)

    println(s"Active [$activeSpan].")

    activeSpan
  }

  @OnMethodExit(onThrowable = classOf[Throwable])
  def onExit(@This statementMixin: StatementAware,
             @Origin method: Method,
             @Thrown throwable: Throwable,
             @Enter activeSpan: ActiveSpan): Unit = {
    if (throwable != null) {
      activeSpan.addSpanTag("error", value = true)
      activeSpan.addSpanTag("throwable", throwable.toString)
    }

    activeSpan.deactivate()
    activeSpan.finish()

    println(s"Finishing [$activeSpan]")

    statementMixin.inFlightStatements.decrement()
  }

  object StatementTypes {
    val Query = "query"
    val Update = "update"
    val Batch = "batch"
    val GenericExecute = "generic-execute"
  }

}
