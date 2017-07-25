/* =========================================================================================
 * Copyright © 2013-2014 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file
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

import java.sql.DriverManager
import java.util.concurrent.Executors

import kamon.jdbc.{SlowQueryProcessor, SqlErrorProcessor}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.{Await, ExecutionContext, Future}

class StatementInstrumentationSpec extends WordSpec with Matchers with Eventually with SpanSugar with BeforeAndAfterAll {
  val connection = DriverManager.getConnection("jdbc:h2:mem:jdbc-spec;MULTI_THREADED=1", "SA", "")
  implicit val parallelQueriesExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  override protected def beforeAll(): Unit = {
    connection
      .prepareStatement("CREATE TABLE Address (Nr INTEGER, Name VARCHAR(128));")
      .executeUpdate()

    connection
      .prepareStatement("CREATE ALIAS SLEEP FOR \"java.lang.Thread.sleep(long)\"")
      .executeUpdate()
  }

  "the StatementInstrumentation" should {
    "track in-flight operations" in {
      Thread.sleep(10000)
      val operations = for (id ← 1 to 10) yield {
        Future {
          DriverManager
            .getConnection("jdbc:h2:mem:jdbc-spec", "SA", "")
            .prepareStatement(s"SELECT * FROM Address where Nr = $id; CALL SLEEP(500)")
            .execute()
        }
      }

      Await.result(Future.sequence(operations), 2 seconds)
    }
  }

}

class NoOpSlowQueryProcessor extends SlowQueryProcessor {
  override def process(sql: String, executionTimeInMillis: Long, queryThresholdInMillis: Long): Unit = { /*do nothing!!!*/ }
}

class NoOpSqlErrorProcessor extends SqlErrorProcessor {
  override def process(sql: String, ex: Throwable): Unit = { /*do nothing!!!*/ }
}