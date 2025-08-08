package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.runtime.plugin.annotations.Plugin

import scala.io.Source

@Plugin(
  id = "removeRemoteStopWords",
  categories = Array("Filter"),
  label = "Remove stop words (remote stop word list)",
  description =
    "Removes stop words from all values." +
      " The stop word list is retrieved via a HTTP connection" +
      " (e.g. https://raw.githubusercontent.com/stopwords-iso/stopwords-de/refs/heads/master/stopwords-de.txt)." +
      " Such an overridable stop word list file is useful, for instance, for specifying the stop words of a different" +
      " language, such as German instead of the" +
      " [default](https://gist.githubusercontent.com/rg089/35e00abf8941d72d419224cfd5b5925d/raw/12d899b70156fd0041fa9778d657330b024b959c/stopwords.txt)" +
      " for the English language." +
      " Each line in the stop word list should contain a single stop word." +
      " The separator defines a regular expression (regex) that is used for detecting words." +
      " By default, the separator is a regular expression for non-whitespace characters." +
      " Additionally, notice the simpler filter 'removeDefaultStopWords', which uses a default stop word list."
)
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
