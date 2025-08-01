package org.silkframework.plugins.filter

import org.silkframework.rule.plugins.transformer.filter.RemoveStopWords
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "removeDefaultStopwords",
  categories = Array("Filter"),
  label = "Remove default stopWords",
  description = "Removes default stop words from all values."
)
case class RemoveDefaultStopWordsTransformer() extends RemoveStopWords()
