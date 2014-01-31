package de.fuberlin.wiwiss.silk.preprocessing

import java.io.File
import de.fuberlin.wiwiss.silk.preprocessing.config.Config
import de.fuberlin.wiwiss.silk.preprocessing.extractor.{FeatureValuePairs, BagOfWords}
import de.fuberlin.wiwiss.silk.preprocessing.transformer.{Tokenizer, Transformer}
import de.fuberlin.wiwiss.silk.preprocessing.entity.{Entity, Property}
import de.fuberlin.wiwiss.silk.preprocessing.execution.ExecuteTask


/**
 * Executes the complete Free Text Preprocessor workflow.
 */
object Sftp {


  /**
   * Main method to allow Free Text Preprocessor to be started from the command line.
   */
  def main(args: Array[String]) {

    //Get the config file
    /*val configFile = System.getProperty("configFile") match {
      case fileName: String => new File(fileName)
      case _ => throw new IllegalArgumentException("No configuration file specified. Please set the 'configFile' property")
    }*/

    val configFile =new File("silk2/silk-tools/silk-freetext-preprocessing/src/main/resources/de/fuberlin/wiwiss/silk/preprocessing/example/products.xml")

    //Load the configuration
    val config = Config.load(configFile)

    //Execute the workflow
    new ExecuteTask().execute(config)
  }


}
