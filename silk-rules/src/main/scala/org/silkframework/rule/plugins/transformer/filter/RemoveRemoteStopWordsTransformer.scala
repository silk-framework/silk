package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

import scala.io.Source

@Plugin(
  id = "removeRemoteStopWords",
  categories = Array("Filter"),
  label = "Remove remote stop words",
  description = "Removes stop words based on a stop word list remote URL.",
  documentationFile = "RemoveRemoteStopWordsTransformer.md"
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("To be or not to be", "that is the question"),
    output = Array("To", "question")
  ),
  new TransformExample(
    input1 = Array("It always seems impossible", "until it's done"),
    output = Array("It impossible", "")
  )
))
case class RemoveRemoteStopWordsTransformer(@Param(value = "URL of the stop word list") stopWordListUrl: String = RemoveRemoteStopWordsTransformer.defaultStopWordListUrl,
                                            @Param(value = "RegEx for detecting words") separator: String = "[\\s-]+")
  extends RemoveStopWords(separator, RemoveRemoteStopWordsTransformer.loadStopWords(stopWordListUrl))

object RemoveRemoteStopWordsTransformer {
  private val defaultStopWordListUrl = "https://raw.githubusercontent.com/stopwords-iso/stopwords-en/refs/heads/master/stopwords-en.txt"

  private def loadStopWords(stopWordListUrl: String): Set[String] = {
    val html = Source.fromURL(stopWordListUrl)
    try {
      html.getLines().toSet
    } finally {
      html.close()
    }
  }
}
