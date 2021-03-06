# Cerberus Realtime evaluator with logistic regression


## Introduction
This small project demonstrates how to use a Machine learning evaluator for real time use case.
This code shoes how to train a Logistic Regression model using Spark-ML to evaluate the chance of an HTTP request to suceed.
The Spark-ML does fair job predicting for a given dataset but do not handle real time evaluation with very low latency.
By using hybrid solution Spark/PMML we get the power of spark to train a very big model and the power of PMML to evaluate 
with very low latency.


## Building model with Spark-ML

1. Create a DataSet for all the features you need to build your model
```
import org.apache.spark.sql.DataFrame

def createPredictionDataSet(df : DataFrame, tableName : String) = {
  df.registerTempTable(tableName)
  sqlContext.sql(s"""SELECT feature1,
         feature2,
         ....
        from $tableName"""
)
}

val features = createPredictionDataSet(myDataSet, "")
```

2. Split your previous data sets in 3 subsets
```
val Array(trainData,cvData,testData) = data.randomSplit(Array(0.8,0.1,0.1))
trainData.cache()
cvData.cache()
testData.cache()
```

3. In case of categorical features (non numeric features) build a OneHotEncoder
```
val myFeatureIndexer = new StringIndexer()
  .setInputCol("column name in Set")
  .setOutputCol("Index")
  .fit(data)
  .setHandleInvalid("skip")

val myEncoder = new OneHotEncoder()
  .setInputCol("Index")
  .setOutputCol("Vector")
```

4. Build an assembler with all the features to train
```
 val featureToTrain  = Array("feature1","feature2","Vector" .....)
 
 import org.apache.spark.mllib.linalg._
 import org.apache.spark.mllib.regression._
 import org.apache.spark.ml.feature.VectorAssembler
 
 val assembler = new VectorAssembler()
   .setInputCols(featureToTrain)
   .setOutputCol("features")
```
 
 5. Build an ML Pipeline to join all the operations together. In this sample the Label column to check is named "isWinning"
```
 import org.apache.spark.ml.classification.LogisticRegression
 import org.apache.spark.ml.Pipeline
 import org.apache.spark.ml.param.ParamMap
 
 val lr = new LogisticRegression()
   .setFeaturesCol("features")
   .setLabelCol("isWinning")
 
 val pipeline = new Pipeline().setStages(Array(myFeatureIndexer, myEncoder, .... , lr))
```
 
 6. Train the model using Spark, you need to find here the best params for your model and test it after. 
 We train the data on the the first dataset we made previously.
 The paramMap includes some parameters related to the logistic regression itself. Here you'll need to find
 what the best value for your use case.
 You can find the Math explanation [here](https://en.wikipedia.org/wiki/Elastic_net_regularization) 
```
 val paramMap = ParamMap(lr.maxIter -> 300)
                .put(lr.regParam -> 0.3, lr.elasticNetParam -> 0.00007482741259139502) // Specify multiple Params.
 
 val pipelineLR = pipeline.fit(trainData, paramMap)
```
 
 7. Write your model as Spark ML model (by default a Parquet File). 
 Unfortunately Spark ML doesn't support to write a PMML model directly. 
 We'll need to convert the model by j-PMML library that support dataframe.
```
 pipelineLR.save("/MyModel")
```
 
 ## Transform a Spark-ML model to PMML model
 Now we can use the util class `MLToPMMLConverter` this take as input a Parquet ML Dataframe and produce a PMML.xml file
  this file can be used by jPMML
Here you'll need to define by yourself the structure model of your model because PMML can't introspect this for you.
The type of each features is also very important.
```  
  `val schemaFilterFeatures = new StructType(Array(
       StructField("feature1", StringType, nullable = false),` ...
```       
 
 This converter will read the SparkML model directly from Amazon S3 and write a PMML model
    
    
 ## Using the real time evaluator 
 When you've a PMML.XML file you can embedded the engine module in your own code. This is actually enclosed in a 
  akka actor to provide a asynchronous processing without too much effort of course you can change this.

 A real sample that use the evaluator can be found `PmmlEvaluatorStarter`
 This sample read a file of features we want to evaluate from a CSV file after parsing the data file
  
  
 ## build the project
 sbt package 
  
 
