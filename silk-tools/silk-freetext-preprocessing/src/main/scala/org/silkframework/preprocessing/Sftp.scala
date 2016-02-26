package org.silkframework.preprocessing

import java.io.File


/**
 * Executes the complete Free Text Preprocessor workflow.
 */
object Sftp {


  /**
   * Main method to allow Free Text Preprocessor to be started from the command line.
   */
  def main(args: Array[String]) {

    //Get the config file
    val configFile = System.getProperty("configFile") match {
      case fileName: String => new File(fileName)
      case _ => throw new IllegalArgumentException("No configuration file specified. Please set the 'configFile' property")
    }

    //Load the configuration
    val config = Config.load(configFile)

    //Execute the workflow
    new ExecuteTask().execute(config)
  }


}
