package com.inneractive.cerberus

import akka.actor.{Actor, Props}
import com.typesafe.config.Config

import scala.concurrent.ExecutionContextExecutor

/**
  * Actor can be used to load a PMML model and evaluate a vector of feature to get prediction
  *
  * Created by Richard Grossman on 2016/07/27.
  */
object MLRealTimeActor {


  /**
    * Create actor props
    * @param dateModel the filename of the PMML data model
    * @param tsc Actor system configuration
    * @return actor Props
    */
  def apply(dateModel: String, tsc: Config) = {
    val modelPath = tsc.getString("cerberus.engine.modellocation") + "/" + dateModel
    val threshold = if (tsc.getDouble("cerberus.engine.threshold") == -1.0) None
    else Some(tsc.getDouble("cerberus.engine.threshold"))

    val mlEvaluator: LRPmmlEvaluator = new LRPmmlEvaluator(modelPath, threshold)

    Props(classOf[MLRealTimeActor], mlEvaluator)
  }
}



class MLRealTimeActor(mlEvaluator: LRPmmlEvaluator) extends Actor {
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  override def receive: Receive = {
    case f: Features => sender ! mlEvaluator.predict(f)
  }
}



