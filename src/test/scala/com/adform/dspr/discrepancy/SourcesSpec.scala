package com.adform.dspr.discrepancy

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import com.adform.dspr.discrepancy.Main.compare
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class SourcesSpec extends FreeSpec with Matchers {
  import Main.{flow, source}

  "Smoke Test" in {
    implicit val system: ActorSystem = ActorSystem("discrepancy-test")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val context = new Context(EnvironmentContext.loadConfig("test"))
    val stream = source(context).via(flow(context)).take(1000).runForeach(println)

    val program = for {
      _ <- stream
      _ <- system.terminate()
    } yield ()

    Await.result(program, 1.hour)

  }

  "Flow Test" in {
    implicit val system: ActorSystem = ActorSystem("discrepancy-test")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val context = new Context(EnvironmentContext.loadConfig("test"))

    val (pub, sub) = TestSource.probe[CookieProfile]
        .via(new AerospikeFlow(context.aerospike))
        .map(compare)
        .toMat(TestSink.probe[ComparisonResult])(Keep.both)
        .run()

    sub.request(n = 3)
    pub.sendNext(CookieProfile(3449673010790253901L))
    pub.sendNext(CookieProfile(2809836353789881901L))
    pub.sendNext(CookieProfile(2455370571455925001L))
    sub.expectNextUnordered(ComparisonResult(false), ComparisonResult(false), ComparisonResult(false))
  }
}