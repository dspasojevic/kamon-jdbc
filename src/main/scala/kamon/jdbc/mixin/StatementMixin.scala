/*
 * =========================================================================================
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

package kamon.jdbc.mixin

import kamon.Kamon
import kamon.agent.api.instrumentation.Initializer
import kamon.metric.{Counter, Histogram, MinMaxCounter}

import scala.beans.BeanProperty

trait StatementAware {
  def inFlightStatements: MinMaxCounter
  def queries: Histogram
  def updates: Histogram
  def batches: Histogram
  def genericExecute: Histogram
  def slowStatements: Counter
  def errors: Counter
  def setSql(sql: Option[String]): Unit
  def getSql: Option[String]
}

class StatementMixin extends StatementAware {
  def inFlightStatements: MinMaxCounter =
    Kamon.minMaxCounter("in-flight-statements")
  def queries: Histogram = Kamon.histogram("queries")
  def updates: Histogram = Kamon.histogram("updates")
  def batches: Histogram = Kamon.histogram("batches")
  def genericExecute: Histogram = Kamon.histogram("generic")
  def slowStatements: Counter = Kamon.counter("slow")
  def errors: Counter = Kamon.counter("errors")

  @BeanProperty
  @volatile var sql: Option[String] = _

  @Initializer
  def _init(): Unit = sql = None
}
