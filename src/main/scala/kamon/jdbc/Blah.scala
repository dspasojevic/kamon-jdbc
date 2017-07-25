package kamon.jdbc

import java.sql.DriverManager
import java.util.concurrent.Executors

import kamon.Kamon

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Created by dan on 21/7/17.
  */
object Blah extends App {

  kamon.Kamon.reconfigure(Kamon.config())

  Kamon.loadReportersFromConfig()

  val span = Kamon.buildSpan("test").startActive()

  try {

    val connection = DriverManager.getConnection("jdbc:h2:mem:jdbc-spec;MULTI_THREADED=1", "SA", "")
    implicit val parallelQueriesExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

    val span = Kamon.buildSpan("inner-test").startActive()
    println("Creating address")
    Thread.sleep(400)

    connection
      .prepareStatement("CREATE TABLE Address (Nr INTEGER, Name VARCHAR(128));")
      .executeUpdate()

    println("Creating function")

    connection
      .prepareStatement("CREATE ALIAS SLEEP FOR \"java.lang.Thread.sleep(long)\"")
      .executeUpdate()

    println("Operating")

    connection.createStatement().execute(s"SELECT * FROM Address where Nr = 1; CALL SLEEP(500)")

    (1 to 10).foreach { id =>
      println(s"Sending [$id]")
      DriverManager
        .getConnection("jdbc:h2:mem:jdbc-spec", "SA", "")
        .prepareStatement(s"SELECT * FROM Address where Nr = $id; CALL SLEEP(500)")
        .execute()
      println(s"Done [$id]")
    }
    span.deactivate()
    span.finish()

    println("Shutting down.")

  }
  finally {
    span.finish()
    span.deactivate()
  }

  Thread.sleep(10000)

  Kamon.stopAllReporters()
  Kamon.scheduler().shutdown()

  System.exit(0)
}
