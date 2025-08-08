package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.runtime.plugin.annotations.Plugin

import scala.io.Source

@Plugin(
  id = "removeRemoteStopWords",
  categories = Array("Filter"),
  label = "Remove stop words (remote stop word list)",
  description = "Removes stop words from all values.",
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
case class RemoveRemoteStopWordsTransformer(stopWordListUrl: String = RemoveRemoteStopWordsTransformer.defaultStopWordListUrl,
                                            separator: String = "[\\s-]+")
  extends RemoveStopWords(separator, RemoveRemoteStopWordsTransformer.loadStopWords(stopWordListUrl))

object RemoveRemoteStopWordsTransformer {
  private val defaultStopWordListUrl = "https://gist.githubusercontent.com/rg089/35e00abf8941d72d419224cfd5b5925d/raw/12d899b70156fd0041fa9778d657330b024b959c/stopwords.txt"

  private def loadStopWords(stopWordListUrl: String): Set[String] = {
    val html = Source.fromURL(stopWordListUrl)
    try {
      html.getLines().toSet
    } finally {
      html.close()
    }
  }
}
