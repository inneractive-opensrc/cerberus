package com.inneractive.cerberus

import java.io.{File, FileOutputStream}
import javax.xml.transform.stream.StreamResult

import com.typesafe.config.Config
import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types._
import org.apache.spark.{SparkConf, SparkContext}
import org.dmg.pmml.PMML
import org.slf4j.{Logger, LoggerFactory}

/**
  * Convert a MLLib datamodel based on dataframe calculated on SPARK to a PMML XML model
  *
  * Created by Richard Grossman on 2016/11/28.
  */
object MLToPMMLConverter extends App with CliStarter {

  /**
    * Schema of Dataframe inside Mllib dataframe
    */
  val schemaFilterFeatures = new StructType(Array(
    StructField("feature1", StringType, nullable = false),
    StructField("feature2", StringType, nullable = false)
  ))

  implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val (appConfig, tsc) = cliAnalyse(args)

  val conf: SparkConf = new SparkConf()
    .setAppName(tsc.getString("cerberus.spark.appname"))
    .setMaster(tsc.getString("cerberus.spark.master"))
  implicit val sc = new SparkContext(conf)

  val s3Helper = S3Helper()

  val localBaseLocation: String = s"${tsc.getString("cerberus.engine.modellocation")}"
  val S3BaseKeyLocation : String = s"${tsc.getString("cerberus.S3.modellocation")}/${appConfig.get.dateModel}"
  val localModelLocation = s"$localBaseLocation/MLModel/${appConfig.get.dateModel}"

  log.info(s"Local Model : $localModelLocation")
  implicit val sqlContext = new SQLContext(sc)

  s3Helper.downloadModel(S3BaseKeyLocation, localBaseLocation)

  log.info(s"Start Spark engine to load from $localModelLocation")
  val sparkModel: PipelineModel = PipelineModel.load(s"$localModelLocation/model/LR")

  val pmmlModelLocation = new File(s"$localModelLocation/model/PMML")
  if (!pmmlModelLocation.exists()) pmmlModelLocation.mkdirs()
  val pmmlLocalFile = new File(pmmlModelLocation, "PMML.xml")

  log.info(s"Write PMML file at $pmmlLocalFile")

  val pmmModel: PMML = org.jpmml.sparkml.ConverterUtil.toPMML(schemaFilterFeatures, sparkModel)
  val modelOutputStream = new FileOutputStream(pmmlLocalFile)
  org.jpmml.model.JAXBUtil.marshalPMML(pmmModel, new StreamResult(modelOutputStream))
  modelOutputStream.flush()
  modelOutputStream.close()

  // Move PMML to S3
  s3Helper.uploadModel(s"MLModel/${appConfig.get.dateModel}/model/PMML/PMML.xml", pmmlLocalFile)
}

import awscala._
import awscala.s3._

case class S3Helper(implicit config: Config, log : Logger) {
  val accessKey: String = config.getString("cerberus.S3.accesskey")
  val secretKey: String = config.getString("cerberus.S3.secretkey")
  val bucketName: String = config.getString("cerberus.S3.bucketname")

  implicit val s3: S3 = S3(accessKey, secretKey)(Region.US_EAST_1)
  val s3Bucket: Option[Bucket] = s3.bucket(bucketName)

  def uploadModel(bucketKey: String, pmmlFile: File): Unit = {
    log.info(s"Start S3 Uploading $pmmlFile to $bucketKey")
    val putObjectResult = s3Bucket map {
      _.put(bucketKey, pmmlFile)
    }
    putObjectResult foreach println
  }

  def downloadModel(modelKeyS3: String, localDirectory: String): Unit = {
    log.info(s"Start downloading $modelKeyS3 to $localDirectory")
    s3Bucket foreach { b =>
      val objectSummary = b.objectSummaries(modelKeyS3)

      objectSummary foreach { o =>
        val content = b.getObject(o.getKey)
        content foreach { c =>
          val fileToWrite = new File(localDirectory, o.getKey)
          val parentDirectory = new File(fileToWrite.getParent)
          if (!parentDirectory.exists()) parentDirectory.mkdirs()

          val fos = new FileOutputStream(new File(localDirectory, o.getKey))
          fos.write(
            Stream.continually(c.content.read).takeWhile(-1 !=).map(_.toByte).toArray
          )
          fos.flush()
          fos.close()
        }
      }
    }
  }
}
