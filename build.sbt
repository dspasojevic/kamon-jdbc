
/* =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
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


val kamonCore = "io.kamon" %% "kamon-core" % "1.0.0-RC1-450978b92bc968bfdb9c6470028ad30586433609"
val kamonAgent = "io.kamon" % "kamon-agent" % "0.0.5-SNAPSHOT"
val kamonAgentScalaExtensions = "io.kamon" %% "agent-scala-extension" % "0.0.5-1958130e3228e35d744a44b6e7eae86b46a1b99b"
val kamonJaeger = "io.kamon" %% "kamon-jaeger" % "1.0.0-RC1-9eec74a0c7f4332336928431852104cc9ad19373"
val h2 = "com.h2database" % "h2" % "1.4.182"
val hikariCP = "com.zaxxer" % "HikariCP" % "2.6.0"


lazy val root = (project in file("."))
  .enablePlugins(JavaAgent)
  .settings(name := "kamon-jdbc")
  .settings(aspectJSettings: _*)
  .settings(
    resolvers += Resolver.mavenLocal,
    resolvers += "scalaz-bintray" at "https://dl.bintray.com/kamon-io/snapshots/",
    javaAgents += "io.kamon" % "kamon-agent" % "0.0.5-SNAPSHOT" % "runtime",
    libraryDependencies ++=
      compileScope(kamonCore, kamonAgent, kamonAgentScalaExtensions, h2, kamonJaeger, slf4jApi) ++
        providedScope(aspectJ, hikariCP) ++
        testScope(h2, scalatest, slf4jApi, logbackClassic))