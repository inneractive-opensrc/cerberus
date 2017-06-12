package com.inneractive.cerberus.example

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.inneractive.cerberus._
import kamon.Kamon
import org.slf4j.LoggerFactory

import scala.io.Source
import scala.util.Try


/**
  * This code is a example how to use the MLEvaluator
  * We read a file with a given structure and build a vector of features
  * the same features we train the model.
  * After calling the evaluator we get a response as a Prediction object.
  * To check the accuracy of the model we can add to the file the real value and compare
  * to predicted value
  */
object PmmlEvaluatorStarter extends App with CliStarter {
  Kamon.start()

  import scala.language.implicitConversions
  implicit def doubleToResult(r : Double): EvalResult = if (r == 1.0) NEGATIVE else POSITIVE

  val (appConfig, tsc) = cliAnalyse(args)
  val actorSystem = ActorSystem("mlEngine")

  implicit val execContext = actorSystem.dispatcher

  val log = LoggerFactory.getLogger("PMML Evaluator")

  val evalActor = actorSystem.actorOf(RoundRobinPool(5).props(MLRealTimeActor(appConfig.get.dateModel,
    actorSystem.settings.config))
    , "evalActor")

  implicit val timeout = Timeout(1, TimeUnit.SECONDS)

  var count = 0
  for (line <- Source.fromFile("/DataSetToEvaluate/*.csv").getLines()) {
    if (count > 0) {
      val f = line.split(",")

      Try {
        M2MFeatures(f(10).toLong, f(1), f(2).toInt, f(3), f(4), f(5).toInt, f(6).toInt, f(7),
          if (f(8) == "0") false else true,
          if (f(9) == "0") false else true,
          f(11).toDouble)
      } map { p =>
        val isWinning = f(12).toDouble
        val features = FeaturesForTraining(p, Some(false)).toIndexed

        val future = (evalActor ? TrainingFeatures(features.toMap(), isWinning)).mapTo[Try[Prediction]]

        // Handle the future as you want

        Thread.sleep(1)
      }
    }

    count = count + 1
  }
}
