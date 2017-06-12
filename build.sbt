import sbt.Keys._
import sbt._

def excludeFromAll(items: Seq[ModuleID], group: String, artifact: String) =
  items.map(_.exclude(group, artifact))

lazy val commonSettings = Seq(
  organization := "com.inneractive.cerberus",
  version := "1.7",
  scalaVersion := "2.10.4",
  isSnapshot := true,
  resolvers += Resolver.sonatypeRepo("public"),
  ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true)},
  resolvers += "Artifactory" at "http://ci.inner-active.com/artifactory/simple/ext-snapshot-local/",
  publishTo := Some("Artifactory Realm" at "http://ci.inner-active.com/artifactory/simple/ext-snapshot-local/"),
  credentials += Credentials("Artifactory Realm", "ci.inner-active.com", "richiesgr", "xeiwbf8v")
)

lazy val commonDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.10",
  "com.github.nscala-time" %% "nscala-time" % "2.12.0",
  "io.kamon" %% "kamon-statsd" % "0.5.2",
  "commons-lang" % "commons-lang" % "2.6"
)

lazy val engineDependencies = Seq(
  "org.jpmml" % "jpmml-sparkml" % "1.0.4" exclude("log4j", "log4j"),
  "org.jpmml" % "pmml-evaluator" % "1.2.14" exclude("log4j", "log4j")
)

lazy val utilsDependencies = Seq(
  "com.github.scopt" %% "scopt" % "3.5.0" exclude("log4j", "log4j"),
  "org.apache.spark" %% "spark-mllib" % "1.6.2" excludeAll(
    ExclusionRule(organization = "org.jpmml"),
    ExclusionRule(organization = "log4j")
  ),
  "com.databricks" %% "spark-csv" % "1.4.0" exclude("log4j", "log4j"),
  "com.github.seratch" %% "awscala" % "0.5.+" excludeAll(
    ExclusionRule(organization = "com.fasterxml.jackson.core"),
    ExclusionRule(organization = "com.fasterxml.jackson.dataformat"),
    ExclusionRule(organization = "com.fasterxml.jackson.module")
  )
)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "cerberus"
  ).aggregate(engine, utils).dependsOn(engine, utils)

lazy val engine = (project in file("engine")).
  settings(commonSettings: _*).
  settings(
    name := "engine",
    libraryDependencies ++= commonDependencies ++ engineDependencies
  )


lazy val utils = (project in file("utils")).
  settings(commonSettings: _*).
  settings(
    name := "utils",
    libraryDependencies ++= commonDependencies ++ utilsDependencies
  ).dependsOn(engine)
