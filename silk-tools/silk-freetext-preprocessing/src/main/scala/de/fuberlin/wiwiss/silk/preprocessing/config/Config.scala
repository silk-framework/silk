package org.silkframework.preprocessing.config

import org.silkframework.runtime.serialization.ValidatingXMLReader

import scala.xml.Node

import scala.Some
import org.silkframework.preprocessing.dataset.Dataset
import org.silkframework.preprocessing.extractor.Extractor

/**
 * A Free text preprocessing configuration.
 * Specifies the whole workflow of the free text preprocessing.
 *
 * @param trainigDatasetSource The training data source
 * @param datasetSource The free text data source
 * @param extractorConfigs The Silk link specifications
 * @param outputSource The output source
 */
case class Config(trainigDatasetSource : InputConfig,
                  datasetSource: InputConfig,
                  extractorConfigs: Traversable[ExtractorConfig],
                  outputSource: Option[OutputConfig]){


  private val extractorsMap = extractorConfigs.map(ex => (ex.id,ex)).toMap

  def extractorConfig(key:String) = extractorsMap.get(key)
}

object Config{
  private val schemaLocation = "de/fuberlin/wiwiss/silk/preprocessing/PreprocessingSpecification.xsd"

  def load = {
    new ValidatingXMLReader(fromXML, schemaLocation)
  }
  /**
   * Reads a configuration from XML.
   */
  def fromXML(node: Node):Config = {

    val trainingDataset = (node \ "Inputs" \ "TrainingDataset").map(InputConfig.fromXML).head
    val dataset = (node \ "Inputs" \ "Dataset").map(InputConfig.fromXML).head
    val extractors = (node \ "ExtractionJob" \ "Extractor").map(ExtractorConfig.fromXML)
    val output = (node \ "Outputs" \ "Output").map(OutputConfig.fromXML).headOption

    Config(trainingDataset, dataset, extractors, output)
  }
}
