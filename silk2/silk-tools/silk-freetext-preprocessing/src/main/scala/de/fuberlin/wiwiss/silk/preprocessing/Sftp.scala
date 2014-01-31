package de.fuberlin.wiwiss.silk.preprocessing

import java.io.File
import de.fuberlin.wiwiss.silk.preprocessing.config.Config
import de.fuberlin.wiwiss.silk.preprocessing.extractor.{FeatureValuePairs, BagOfWords}
import de.fuberlin.wiwiss.silk.preprocessing.transformer.{Tokenizer, Transformer}
import de.fuberlin.wiwiss.silk.preprocessing.entity.{Entity, Property}
import de.fuberlin.wiwiss.silk.preprocessing.execution.ExecuteTask


/**
 * Created with IntelliJ IDEA.
 * User: Petar
 * Date: 25/12/13
 * Time: 15:14
 * To change this template use File | Settings | File Templates.
 */
object Sftp {


  /**
   * Main method to allow Free Text Preprocessor to be started from the command line.
   */
  def main(args: Array[String]) {

    val configFile = System.getProperty("configFile") match {
      case fileName: String => new File(fileName)
      case _ => throw new IllegalArgumentException("No configuration file specified. Please set the 'configFile' property")
    }

    val config = Config.load(configFile)
    new ExecuteTask().execute(config)
  }


}
