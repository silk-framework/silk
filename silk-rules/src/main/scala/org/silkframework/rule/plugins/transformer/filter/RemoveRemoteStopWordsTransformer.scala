package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.runtime.plugin.annotations.Plugin

import scala.io.Source

@Plugin(
  id = "removeRemoteStopwords",
  categories = Array("Filter"),
  label = "Remove stopwords (remote stopword list)",
  description = "Removes stopwords from all values. The stopword list is retrieved via a http connection (e.g. https://sites.google.com/site/kevinbouge/stopwords-lists/stopwords_de.txt). Each line in the stopword list contains a stopword. The separator defines a regex that is used for detecting words."
)
case class RemoveRemoteStopWordsTransformer(stopWordListUrl: String, separator: String = "[\\s-]+")
  extends RemoveStopWords(separator) {

  override val stopWords: Set[String] = {
    def loadStopWords(stopWordListUrl: String): Set[String] = {
      val html = Source.fromURL(stopWordListUrl)
      try {
        html.getLines().toSet
      } finally {
        html.close()
      }
    }

    loadStopWords(stopWordListUrl)
  }
}
