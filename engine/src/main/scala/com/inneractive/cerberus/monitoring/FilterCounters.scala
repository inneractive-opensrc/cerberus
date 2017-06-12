package com.inneractive.cerberus.monitoring

/**
  * A mutable container to evaluate the precision / recall of an evaluator
  */
case class FilterCounters(var inputCounter: Long = 0, var filteredCounter: Long = 0,
                          var falseNegativeCounter: Long = 0, var invalidFeatures: Long = 0,
                          var avgPrecision: Double = 0.0, var realValueCounter: Long = 0) {

  private var elapsedTime: Long = System.currentTimeMillis()

  def init(): Unit = {
    inputCounter = 0
    filteredCounter = 0
    falseNegativeCounter = 0
    elapsedTime = System.currentTimeMillis()
    invalidFeatures = 0
    avgPrecision = 0.0d
    realValueCounter = 0
  }

  def update(requestCounter: Long, filteredCounter: Long, falsePositiveCounter: Long,
             invalidFeatures: Long, avgPrecision: Double, realAdCounter: Long): Unit = {
    this.inputCounter = this.inputCounter + requestCounter
    this.filteredCounter = this.filteredCounter + filteredCounter
    this.falseNegativeCounter = this.falseNegativeCounter + falsePositiveCounter
    this.invalidFeatures = this.invalidFeatures + invalidFeatures
    this.avgPrecision = this.avgPrecision + avgPrecision
    this.realValueCounter = this.realValueCounter + realAdCounter
  }

  def update(f: FilterCounters): Unit = {
    update(f.realValueCounter, f.filteredCounter, f.falseNegativeCounter, f.invalidFeatures, f.avgPrecision, f.realValueCounter)
  }

  def startedAt(): Long = elapsedTime
}
