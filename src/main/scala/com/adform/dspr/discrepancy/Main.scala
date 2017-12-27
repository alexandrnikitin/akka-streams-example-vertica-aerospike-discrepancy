package com.adform.dspr.discrepancy

import java.sql.ResultSet
import java.time.LocalDateTime

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Main {

  def main(args: Array[String]): Unit = {
    val context = new Context()

    implicit val system = ActorSystem("discrepancy")
    implicit val materializer = ActorMaterializer()

    val stream = source(context).via(flow(context)).toMat(sink())(Keep.right)

    val program = for {
      _ <- stream.run()
      _ <- system.terminate()
    } yield ()

    Await.result(program, 1.hour)
  }

  def source(context: Context): Source[PartitionedQuery, NotUsed] = {
    val numberOfPartitions = context.application.numberOfPartitions
    val partitions = 0.until(numberOfPartitions).map(Partition(_, numberOfPartitions))
    val dateTimeRange = VerticaSource.getDateTimeRange(context.vertica, java.time.Duration.ofHours(1))
    Source(partitions.map(PartitionedQuery(_, dateTimeRange)))
  }

  def flow(context: Context): Flow[PartitionedQuery, (CookieProfile, Option[CookieProfile]), NotUsed] = {
    Flow[PartitionedQuery]
        .flatMapConcat(q => new VerticaSource(q, context.vertica, resultSetToCookieProfile))
        .filter(cp => cp.cookieId != 0)
        .throttle(5000, 1.second, 500, ThrottleMode.Shaping)
        .via(new AerospikeFlow(context.aerospike))
  }

  def sink(): Sink[(CookieProfile, Option[CookieProfile]), Future[Done]] = Sink.fromGraph(new ComparisonSink())

  val resultSetToCookieProfile: (ResultSet) => CookieProfile = rs => {
    val cookieId = rs.getLong(1)
    val impressions = rs.getLong(2)
    val clicks = rs.getLong(3)
    CookieProfile(cookieId, impressions, clicks)
  }

}

case class PartitionedQuery(partition: Partition, period: (LocalDateTime, LocalDateTime))

case class Partition(number: Int, numberOfPartitions: Int)

case class CookieProfile(cookieId: CookieId, impressions: Long, clicks: Long)

