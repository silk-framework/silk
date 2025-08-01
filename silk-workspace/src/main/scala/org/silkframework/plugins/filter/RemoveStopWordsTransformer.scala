package org.silkframework.plugins.filter

import org.silkframework.rule.plugins.transformer.filter.RemoveStopWords
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.Resource

@Plugin(
  id = "removeStopwords",
  categories = Array("Filter"),
  label = "Remove stopWords",
  description = "Removes stopWords from all values. Each line in the stopword list contains a stopword. The separator defines a regex that is used for detecting words."
)
case class RemoveStopWordsTransformer(stopWordList: Resource, separator: String = "[\\s-]+")
  extends RemoveStopWords(separator, stopWordList.loadAsString().split("\n").toSet) {

  override def referencedResources: Seq[Resource] = Seq(stopWordList)
}
