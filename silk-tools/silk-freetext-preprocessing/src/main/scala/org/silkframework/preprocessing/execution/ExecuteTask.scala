package org.silkframework.preprocessing.execution


import java.util.logging.{Level, Logger}

/**
 * Execute task
 */
class ExecuteTask {

  /**
   * The level at which task status changes should be logged.
   * Examples are status updates when the task is started and stopped.
   */
  var statusLogLevel = Level.INFO


  /**
   * The logger used to log status changes.
   */
  protected val logger = Logger.getLogger(getClass.getName)




  def execute(config:Config){

    val trainigDatasetSource = config.trainigDatasetSource
    val freetextDatasetSource = config.datasetSource
    val extractorConfigs = config.extractorConfigs
    val outputSource = config.outputSource.get

    //Loading the training dataset
    logger.log(statusLogLevel, "Loading the " + trainigDatasetSource.id + "(training) dataset...")
    val trainingDataset = new  Dataset(trainigDatasetSource.id,
      loadEntities(trainigDatasetSource.file, trainigDatasetSource.format))
    logger.log(statusLogLevel, "Loaded the " + trainigDatasetSource.id + "(training) dataset!\nA total number of "
      + trainingDataset.entitySet.size + " entities were loaded.")


    //Loading the "free-text" dataset
    logger.log(statusLogLevel, "Loading the " + trainigDatasetSource.id + "(\"free-text\") dataset...")
    val freetextDataset = new Dataset(freetextDatasetSource.id,
      loadEntities(freetextDatasetSource.file, freetextDatasetSource.format))
    logger.log(statusLogLevel, "Loaded the " + freetextDatasetSource.id + "(\"free-text\") dataset!\nA total number of "
      + freetextDataset.entitySet.size + " entities were loaded.")

    //Setting up for extraction
    logger.log(statusLogLevel, "Setting up the extractors...")
    val extractors = for(extractorConfig <- extractorConfigs) yield {
        resolveExtractor(extractorConfig)
    }
    logger.log(statusLogLevel, "Extractors successfully set up!")

    //Extracting new properties
    logger.log(statusLogLevel, "Extracting...")
    var counter = 1;
    val extractedEntities = for(extractor <- extractors) yield {
      logger.log(statusLogLevel, "Processing the " + extractor.id)
      if(extractor.isInstanceOf[BagOfWords] || extractor.isInstanceOf[FeatureValuePairs]){
        extractor.asInstanceOf[AutoExtractor].train(trainingDataset)
      }
      val entities = extractor.apply(freetextDataset, trainingDataset.findPath)
      logger.log(statusLogLevel, "Completed " + counter + "/" + extractors.size + " extractors")
      counter += 1
      entities
    }

    logger.log(statusLogLevel, "Completed all extractors. Proceeding with merging!")

    //Merging results
    val outputEntities = extractedEntities.reduce(_ ++ _).groupBy(entity => entity.uri).map(pair => merge(pair))


    //Writing to file
    logger.log(statusLogLevel, "Writing to file...")
    val output = new OutputWriter(outputSource.id, outputSource.file, outputSource.format)
    output.write(outputEntities)
    logger.log(statusLogLevel, "Finished!")

  }



  def loadEntities(file: String, format: String) = {
    new JenaSource(file, format).retrieve()
  }

  def resolveExtractor(config: ExtractorConfig):Extractor = {
    val transformers = config.transformers.isDefined match {
      case true => resolveTransformers(config.transformers.get)
      case false => List.empty[Transformer]
    }

    config.method match {
      case "BagOfWords" => new BagOfWords(config.id, config.property, transformers, config.param)
      case "FeatureValuePairs" => new FeatureValuePairs(config.id, config.property, transformers, config.param)
      case "RegexMatch" => new Regex(config.id, config.property, transformers, config.param)
      case "DictionarySearch" => new Dictionary(config.id, config.property, transformers, config.param)
    }
  }


  def resolveTransformers(config: TransformerConfig):List[Transformer] = {
      def resolveTransformersAcc(config: TransformerConfig, transformers: List[Transformer]):List[Transformer] = config.transform match {
        case None => transformers:::List(resolveTransformer(config))
        case Some(tr: TransformerConfig) => resolveTransformersAcc(tr, transformers:::List(resolveTransformer(config)))
      }

      def resolveTransformer(config:TransformerConfig):Transformer = config.method match {
        case "tokenize" => new Tokenizer(config.params("delimiter"))
        case "ngrams" => new Ngrams(config.params("lowerBound").toInt, config.params("upperBound").toInt)
      }

      resolveTransformersAcc(config, List.empty[Transformer]).reverse
  }

  def merge(value: (String,Traversable[Entity])): Entity = {
    new Entity(value._1, (for(entity <- value._2) yield entity.properties).flatMap(x => x))
  }

}
