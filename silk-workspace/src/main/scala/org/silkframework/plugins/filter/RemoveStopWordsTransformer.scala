package org.silkframework.plugins.filter

import org.silkframework.rule.plugins.transformer.filter.RemoveStopWords
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.Resource

@Plugin(
  id = "removeStopWords",
  categories = Array("Filter"),
  label = "Remove stopWords",
  description =
    "Removes stop words from all values." +
      " The stop word list is specified as a resource," +
      " e.g. a file identical to" +
      " https://raw.githubusercontent.com/stopwords-iso/stopwords-de/refs/heads/master/stopwords-de.txt." +
      " Such a stop word list resource is useful, for instance, for specifying the stop words of a specific language" +
      " or application domain." +
      " Each line in the given stop word list resource should contain a single stop word." +
      " The separator defines a regular expression (regex) that is used for detecting words." +
      " By default, the separator is a regular expression for non-whitespace characters." +
      " Additionally, notice the simpler filter 'removeDefaultStopWords', which uses a default stop word list."
)
case class RemoveStopWordsTransformer(stopWordList: Resource, separator: String = "[\\s-]+")
  extends RemoveStopWords(separator, stopWordList.loadAsString().split("\n").toSet) {

  override def referencedResources: Seq[Resource] = Seq(stopWordList)
}
