package com.inneractive.cerberus

import java.io.{File, FileInputStream}
import java.util
import javax.xml.transform.stream.StreamSource

import org.dmg.pmml.{FieldName, Model, PMML}
import org.jpmml.evaluator.{EvaluatorUtil, FieldValue, ModelEvaluator, ModelEvaluatorFactory}
import org.jpmml.model.JAXBUtil

import scala.collection.Map
import scala.util.Try


/**
  * Features are map of values they represent a set of data to be evaluated by the prediction engines
  */
trait Features {
  val vector : Map[String,Any]
}

sealed trait EvalResult

case object POSITIVE extends EvalResult
case object NEGATIVE extends EvalResult

case class EvaluationFeatures(vector : Map[String, Any]) extends Features
case class TrainingFeatures(vector : Map[String, Any], realResult : EvalResult) extends Features

/**
  * Create a model based on jPMML ML library
  *
  * Created by richard on 7/6/16.
  */

/**
  * Trait for prediction results
  */
sealed trait PredictionResult {
  val featuresId: String
  val toFilter: Boolean
  val precision: Double
}

/**
  * Use of class to be compatible with EHCache class loader
  * When no Prediction can be made unseen feature store a NoPrediction
  */
class NoPrediction(val featuresId: String = "NA") extends PredictionResult with Serializable {
  override val toFilter: Boolean = false
  override val precision: Double = 0.0

  override def toString = s"NoPrediction(toFilter=$toFilter, precision=$precision)"
}

/**
  * Prediction Structure
  *
  * @param toFilter  is true the request is not relevant for the evaluator
  * @param precision confidence level for the prediction 0.0 to 1.0
  */
class Prediction(val featuresId: String, val toFilter: Boolean, val precision: Double) extends PredictionResult with Serializable {
  def this(featuresId: String, prediction: Double, precision: Double) = this(featuresId, prediction == 0.0, precision)

  override def toString = s"Prediction(toFilter=$toFilter, precision=$precision)"
  def toCSV = s"$toFilter,$precision"
}

import scala.collection.JavaConversions._

/**
  * Evaluator logistic regression read data from PMML.xml model data file
  *
  * @param modelLocation the base location for the PMML model
  * @param threshold lower to accept the prediction as valid
  */
class LRPmmlEvaluator(modelLocation: String, threshold: Option[Double]) {

  val inputStream = new FileInputStream(new File(modelLocation, "/model/PMML/PMML.xml"))
  val pmmModel: PMML = JAXBUtil.unmarshalPMML(new StreamSource(inputStream))

  val evaluatorFactory: ModelEvaluatorFactory = ModelEvaluatorFactory.newInstance()
  val evaluator: ModelEvaluator[_ <: Model] = evaluatorFactory.newModelManager(pmmModel)
  val activeFields: util.List[FieldName] = evaluator.getActiveFields
  val targetFields: util.List[FieldName] = EvaluatorUtil.getTargetFields(evaluator)
  val outputFields: util.List[FieldName] = EvaluatorUtil.getOutputFields(evaluator)

  def arguments(eval: FieldName => (FieldName, FieldValue)) = (activeFields map eval).toMap

  def getResults(result: java.util.Map[FieldName, _]) = {
    val targets = targetFields map (f => EvaluatorUtil.decode(result.get(f)))
    val outputs = outputFields map (f => result.get(f))
    Tuple3(targets.head.asInstanceOf[Double], outputs.head.asInstanceOf[Double], outputs(1).asInstanceOf[Double])
  }

  def callEvaluator(params : Map[FieldName, _]) = getResults(evaluator.evaluate(params))

  def predict(features: Features) = Try {
    val r = callEvaluator(arguments((f: FieldName) => {
      val value = features.vector(f.getValue)
      val activeValue = evaluator.prepare(f, value)
      (f, activeValue)
    }))
    new Prediction("", if (r._1 == 0.0) true else false, if (r._1 == 0) r._2 else r._3)
  }
}
