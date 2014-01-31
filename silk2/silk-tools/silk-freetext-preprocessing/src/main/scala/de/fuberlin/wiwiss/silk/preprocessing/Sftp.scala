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



  def main(args: Array[String]) {

      val file = new File("/Users/Petar/Documents/IdeaProjects/my-silk-fork/silk2/silk-tools/silk-freetext-preprocessing/src/main/resources/de/fuberlin/wiwiss/silk/preprocessing/example/products.xml")
      val config = Config.load(file)

      new ExecuteTask().execute(config)

  }


}
