package com.adform.dspr.discrepancy

import java.sql.{DriverManager, ResultSet}
import java.time.format.DateTimeFormatter
import java.time.{Clock, Duration, LocalDateTime}

import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import resource._

class VerticaSource(partitionedQuery: PartitionedQuery, settings: VerticaSettings, f: ResultSet => CookieProfile)
    extends GraphStage[SourceShape[CookieProfile]] {
  import VerticaSource._

  val out: Outlet[CookieProfile] = Outlet("VerticaSource.out")
  override val shape: SourceShape[CookieProfile] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) with OutHandler {

      private[this] var resultSet: ResultSet = _

      setHandler(out, this)

      override def preStart(): Unit = {
        val connection = DriverManager.getConnection(settings.url, settings.username, settings.password)
        val statement = connection.createStatement()
        statement.setFetchSize(settings.fetchSize)
        resultSet = statement.executeQuery(query(partitionedQuery))
      }

      override def postStop(): Unit = if (resultSet ne null) resultSet.close()

      override def onPull(): Unit = {
        if (resultSet.next()) {
          push(out, f(resultSet))
        } else {
          complete(out)
        }
      }
    }
  }

}

object VerticaSource {

  val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  implicit class RichResultSet(val row: ResultSet) extends AnyVal {
    def getLongOpt(column: Int): Option[Long] = wasNull(row.getLong(column))

    protected def wasNull[VType](value: VType): Option[VType] = if (row.wasNull()) None else Option(value)
  }

  def query(partitionedQuery: PartitionedQuery) = {

    val partitionNumber = partitionedQuery.partition.number
    val numberOfPartitions = partitionedQuery.partition.numberOfPartitions
    val startDateTime = partitionedQuery.period._1
    val endDateTime = partitionedQuery.period._2

    s"""SELECT cookie_id, historical_imps_seen, historical_clicks_made
       |FROM table
       |WHERE log_time > '${startDateTime.format(dateTimeFormatter)}' AND
       |log_time <= '${endDateTime.format(dateTimeFormatter)}' AND
       |ABS(cookie_id % $numberOfPartitions) = $partitionNumber""".stripMargin
  }
  def getDateTimeRange(settings: VerticaSettings, intervalToTake: Duration): (LocalDateTime, LocalDateTime) = {
    val query = "SELECT MAX(log_time) FROM table"
    var endDateTime = LocalDateTime.now(Clock.systemUTC())

    for {
      connection <- managed(DriverManager.getConnection(settings.url, settings.username, settings.password))
      statement <- managed(connection.createStatement)
      resultSet <- managed(statement.executeQuery(query))
    } {
      resultSet.next()
      endDateTime = resultSet.getTimestamp(1).toLocalDateTime
    }

    (endDateTime.minus(intervalToTake), endDateTime)
  }

}
