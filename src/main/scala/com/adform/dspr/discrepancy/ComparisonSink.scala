package com.adform.dspr.discrepancy

import akka.Done
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}
import ai.x.diff.{DiffShow, Different, Identical}
import ai.x.diff.conversions._

import scala.concurrent.{Future, Promise}

class ComparisonSink
    extends GraphStageWithMaterializedValue[SinkShape[(CookieProfile, Option[CookieProfile])], Future[Done]] {

  val in = Inlet[(CookieProfile, Option[CookieProfile])]("ComparisonSink.in")

  override val shape: SinkShape[(CookieProfile, Option[CookieProfile])] = SinkShape(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val promise = Promise[Done]()
    val logic = new GraphStageLogic(shape) with InHandler {
      private[this] var result = Result(0, 0, 0)

      setHandler(in, this)

      override def preStart(): Unit = pull(in)

      override def onPush(): Unit = {
        val tuple = grab(in)
        val comparison = DiffShow.diff[Option[CookieProfile]](Some(tuple._1), tuple._2)
        comparison match {
          case _: Identical =>
          case c: Different => println(s"${tuple._1.cookieId}: " + c.string)
          case c: ai.x.diff.Error => println(c.string)
        }

        tuple match {
          case (_, None) =>
            result = result.copy(
              result.numberOfCookies + 1,
              result.doesNotExist + 1)
          case (_, Some(_)) if !comparison.isIdentical =>
            result = result.copy(
              result.numberOfCookies + 1,
              result.doesNotExist,
              result.different + 1)
          case (_, Some(_)) =>
            result = result.copy(result.numberOfCookies + 1)
        }

        if (result.numberOfCookies % 100 == 0) {
          println(result)
        }

        pull(in)
      }

      override def onUpstreamFinish(): Unit = {
        super.onUpstreamFinish()
        promise.trySuccess(Done)
      }

      override def onUpstreamFailure(ex: Throwable): Unit = {
        super.onUpstreamFailure(ex)
        promise.tryFailure(ex)
      }
    }

    (logic, promise.future)
  }

  case class Result(numberOfCookies: Long, doesNotExist: Long, different: Long)

}
