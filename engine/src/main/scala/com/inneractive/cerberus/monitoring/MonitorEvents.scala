package com.inneractive.cerberus.monitoring

import com.inneractive.cerberus.{EvalResult, POSITIVE, Prediction, TrainingFeatures}
import com.typesafe.config.Config
import org.joda.time.Period
import org.joda.time.format.ISOPeriodFormat
import org.slf4j.LoggerFactory

import scala.util.Try

trait MonitorEvent {
  def sendResult(prediction : Try[Prediction], f : TrainingFeatures): Unit
}

/**
  * This class can be used to send monitoring using StatsD
  */
class MonitorEventToStatsD(config: Config) extends MonitorEvent {
  import scala.language.implicitConversions
  implicit def resultToLong(r : EvalResult): Long = if (r == POSITIVE) 1 else 0

  val log = LoggerFactory.getLogger(this.getClass)
  val threshold: Double = config.getDouble("cerberus.engine.threshold")
  val logUpdatePeriod: String = config.getString("cerberus.engine.logupdate.period")
  val scheduleAtDuration: Period = ISOPeriodFormat.standard().parsePeriod(logUpdatePeriod)
  val sendToMonitoring: Boolean = config.getBoolean("cerberus.engine.metricmonitoring")
  val sendToLog: Boolean = config.getBoolean("cerberus.engine.sendmetriclogging")


  val counters = FilterCounters()

  /**
    * Receive event of type Prediction and aggregate them to lowered the number of events to send
    *
    * @param prediction the prediction for an evaluation
    * @param f the real value to compare if the prediction was correct
    */
  override def sendResult(prediction : Try[Prediction], f : TrainingFeatures): Unit = {
    // Update the request counter only for each request not only the succeed prediction
    if (prediction.isFailure) counters.update(1, 0, 0, 1, 0.0, f.realResult)
    else counters.update(1, 0, 0, 0, 0.0,  f.realResult)

    log.debug(s"Counters: $counters")

    prediction map { result =>
      val filtered = if (result.toFilter && result.precision >= threshold) 1 else 0
      val falseNegativeCounter = if (filtered == 1 && f.realResult == POSITIVE) 1 else 0
      counters.update(0, filtered, falseNegativeCounter, 0, result.precision, 0)

      if ((System.currentTimeMillis() - counters.startedAt()) >= scheduleAtDuration.toStandardDuration.getMillis) {
        if (sendToMonitoring) MonitorsInstruments.emitCounter(counters)
        if (sendToLog) LoggingInstruments.emitCounter(counters, log)
        counters.init()
      }
    }

  }
}

/**
  * Noop monitoring event
  */
class MonitorEventNoop extends MonitorEvent {
  override def sendResult(prediction: Try[Prediction], f: TrainingFeatures): Unit = {}
}
