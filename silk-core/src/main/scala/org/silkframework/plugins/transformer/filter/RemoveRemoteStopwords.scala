package org.silkframework.plugins.transformer.filter

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource.Resource
import scala.collection.mutable.ListBuffer
import scala.io.Source

@Plugin(
  id = "removeRemoteStopwords",
  categories = Array("Filter"),
  label = "Remove stopwords (remote stopword list)",
  description = "Removes stopwords from all values. The stopword list is retrieved via a http connection (e.g. https://sites.google.com/site/kevinbouge/stopwords-lists/stopwords_de.txt). Each line in the stopword list contains a stopword. The separator defines a regex that is used for detecting words."
)
case class RemoveRemoteStopwords(stopWordListUrl: String, separator: String = "[\\s-]+") extends SimpleTransformer {

  val html = Source.fromURL(stopWordListUrl)
  val reader = html.bufferedReader()
  val stopWords = new ListBuffer[String]
  while (reader.readLine() != null) {
    stopWords+=reader.readLine()
  }

  val regex = separator.r

  override def evaluate(value: String): String = {
    val result = new StringBuilder
    for(word <- regex.split(value) if !stopWords.contains(word)) {
      result.append(word)
      result.append(" ")
    }
    result.toString()
  }
}
