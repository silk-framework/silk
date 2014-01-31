package de.fuberlin.wiwiss.silk.preprocessing.config


import de.fuberlin.wiwiss.silk.util.ValidatingXMLReader
import scala.xml.Node

import scala.Some
import de.fuberlin.wiwiss.silk.preprocessing.dataset.Dataset
import de.fuberlin.wiwiss.silk.preprocessing.extractor.Extractor

/**
 * Created with IntelliJ IDEA.
 * User: Petar
 * Date: 22/01/14
 * Time: 17:01
 * To change this template use File | Settings | File Templates.
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
