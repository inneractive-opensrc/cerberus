package com.inneractive.cerberus.monitoring

import kamon.Kamon
import kamon.metric.instrument.Counter
import org.slf4j.Logger

/**
  * Created by Richard Grossman on 2017/02/13.
  */
object MonitorsInstruments {
  private val requestHistogramMetric: Counter = Kamon.metrics.counter("Request-Histogram")
  private val filterCounterMetric: Counter = Kamon.metrics.counter("FilteredByPrediction-Counter")
  private val falseNegativeCounterMetric: Counter = Kamon.metrics.counter("FalseNegative-Counter")
  private val invalidEvalCounterMetric: Counter = Kamon.metrics.counter("EvalFailure-Counter")
  private val realAdCounterMetric: Counter = Kamon.metrics.counter("RealAd-Counter")

  def emitCounter(f: FilterCounters): Unit = {
    requestHistogramMetric.increment(f.inputCounter)
    filterCounterMetric.increment(f.filteredCounter)
    falseNegativeCounterMetric.increment(f.falseNegativeCounter)
    invalidEvalCounterMetric.increment(f.invalidFeatures)
    realAdCounterMetric.increment(f.realValueCounter)
  }
}

object LoggingInstruments {
  def emitCounter(f: FilterCounters, log: Logger): Unit = {
    log.info(s"${f.inputCounter},${f.realValueCounter},${f.filteredCounter},${f.falseNegativeCounter},${f.invalidFeatures},${f.avgPrecision / f.inputCounter.toDouble}")
  }
}
