package com.adform.dspr.discrepancy

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.aerospike.client.policy.ClientPolicy
import com.aerospike.client.{AerospikeClient, Key}

class AerospikeFlow(settings: AerospikeSettings)
    extends GraphStage[FlowShape[CookieProfile, (CookieProfile, Option[CookieProfile])]] {

  val in = Inlet[CookieProfile]("AerospikeFlow.in")
  val out = Outlet[(CookieProfile, Option[CookieProfile])]("AerospikeFlow.out")

  override val shape = FlowShape.of(in, out)

  override def createLogic(attr: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {
      private[this] var client: AerospikeClient = _

      setHandler(in, this)
      setHandler(out, this)

      override def preStart(): Unit = {
        val policy = new ClientPolicy
        policy.timeout = settings.readTimeout
        policy.writePolicyDefault.maxRetries = 5
        client = new AerospikeClient(policy, settings.hosts: _*)
      }

      override def onPush(): Unit = {
        val inCookieProfile = grab(in)
        val key = new Key(settings.namespace, settings.set, inCookieProfile.cookieId)
        val record = client.get(null, key)
        val outCookieProfile = Option(record).map(r => {
          val impressions = r.getLong("impressions")
          val clicks = r.getLong("clicks")
          CookieProfile(inCookieProfile.cookieId, impressions, clicks)
        })
        push(out, inCookieProfile -> outCookieProfile)
      }

      override def onPull(): Unit = pull(in)

      override def postStop(): Unit = if (client ne null) client.close()
    }
}

