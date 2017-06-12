package com.inneractive.cerberus.example

import org.apache.commons.lang.StringUtils

import scala.language.implicitConversions

/**
  * Created by Richard Grossman on 6/29/16.
  */
sealed trait FeaturesTrait

case class M2MFeatures(publisherId: Long, country: String, age: Int, gender: String,
                       brand: String, height: Int, width: Int, connectionType: String,
                       withBundleId: Boolean, withGeoInfo: Boolean, rtbFloorSent : Double) extends FeaturesTrait

case class RealFeature(features: M2MFeatures, hasGotRealImpression : Boolean)

case class FeaturesForTraining (features: M2MFeatures, isWinning : Option[Boolean]) {
  implicit def bool2int(b: Boolean): Int = if (b) 1 else 0
  implicit def bool2Double(b: Boolean): Double = if (b) 1.0 else 0.0

  def isEmpty(param: String) = if (StringUtils.isEmpty(param)) "NA" else param

  val toIndexed = isWinning map { w =>
    new IndexedTraining(isEmpty(features.publisherId.toString),
      isEmpty(features.country),
      features.age,
      if (features.gender == null || features.gender == "null") "NA" else features.gender,
      isEmpty(features.brand),
      features.height,
      features.width,
      if (features.connectionType == null) "NA" else features.connectionType,
      features.withBundleId: Int,
      features.withGeoInfo: Int,
      features.rtbFloorSent,
      features.hashCode().toString,
      w : Double)
  } getOrElse new IndexedPrediction(isEmpty(features.publisherId.toString),
    isEmpty(features.country),
    Option(features.age) getOrElse (-1),
    Option(features.gender) getOrElse "NA",
    isEmpty(features.brand),
    features.height,
    features.width,
    if (features.connectionType == null || features.connectionType == "(null)") "NA" else features.connectionType,
    features.withBundleId: Int,
    features.withGeoInfo: Int,
    Option(features.rtbFloorSent) getOrElse 0.0,
    features.hashCode().toString)
}

trait FeaturesIndexed extends FeaturesTrait {
  def toMap : Map[String, Any]
}

class IndexedPrediction(publisherId: String, country: String, age: Int, gender: String,
                             brand: String, height: Int, width: Int,
                             conType: String, withBundleId: Int, withGeo: Int,
                             rtbFloorPriceSent : Double, featuresId : String) extends FeaturesIndexed {
  def toMap() = Map("publisherId" -> publisherId, "country" -> country, "age" -> age, "gender" -> gender,
    "brand" -> brand, "height" -> height, "width" -> width, "conType" -> conType, "withBundleId" -> withBundleId,
    "withGeo" -> withGeo, "rtbFloorPriceSent" -> rtbFloorPriceSent, "featuresId" -> featuresId)
}

class IndexedTraining(publisherId: String, country: String, age: Int, gender: String,
                           brand: String, height: Int, width: Int,
                           conType: String, withBundleId: Int, withGeo: Int,
                           rtbFloorPriceSent : Double, featuresId : String, isWinning : Double
                             ) extends IndexedPrediction(publisherId,country,age, gender, brand, height, width, conType, withBundleId,
  withGeo, rtbFloorPriceSent, featuresId) {
  override def toMap() = super.toMap() + ("isWinning" -> isWinning)
}
