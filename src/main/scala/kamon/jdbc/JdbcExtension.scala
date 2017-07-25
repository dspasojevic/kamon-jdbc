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

package kamon.jdbc

import kamon.Kamon

object JdbcExtension {

  val SegmentLibraryName = "jdbc"

  private val config = Kamon.config.getConfig("kamon.jdbc")

  private val nameGeneratorFQN = config.getString("name-generator")
  private val nameGenerator: JdbcNameGenerator = new DefaultJdbcNameGenerator

  private val slowQueryProcessorClass = config.getString("slow-query-processor")
  private val slowQueryProcessor: SlowQueryProcessor = new DefaultSlowQueryProcessor

  private val sqlErrorProcessorClass = config.getString("sql-error-processor")
  private val sqlErrorProcessor: SqlErrorProcessor = new DefaultSqlErrorProcessor

  val slowQueryThreshold = config.getDuration("slow-query-threshold").toMillis
  val shouldGenerateSegments = config.getBoolean("generate-segments")

  def processSlowQuery(sql: String, executionTime: Long) =
    slowQueryProcessor.process(sql, executionTime, slowQueryThreshold)

  def processSqlError(sql: String, ex: Throwable) =
    sqlErrorProcessor.process(sql, ex)

  def generateJdbcSegmentName(statementType: String, statement: String): String =
    nameGenerator.generateJdbcSegmentName(statementType, statement)
}

trait SlowQueryProcessor {
  def process(sql: String, executionTime: Long, queryThreshold: Long): Unit
}

trait SqlErrorProcessor {
  def process(sql: String, ex: Throwable): Unit
}

trait JdbcNameGenerator {
  def generateJdbcSegmentName(statementType: String, statement: String): String
}

class DefaultJdbcNameGenerator extends JdbcNameGenerator {
  def generateJdbcSegmentName(statementType: String, statement: String): String = s"jdbc-$statementType"
}

class DefaultSqlErrorProcessor extends SqlErrorProcessor {

  override def process(sql: String, cause: Throwable): Unit = {
    // FIXME
    println(s"the query [$sql] failed with exception [${cause.getMessage}]")
  }
}

class DefaultSlowQueryProcessor extends SlowQueryProcessor {

  override def process(sql: String, executionTimeInMillis: Long, queryThresholdInMillis: Long): Unit = {
    // FIXME
    println(s"The query [$sql] took $executionTimeInMillis ms and the slow query threshold is $queryThresholdInMillis ms")
  }
}
