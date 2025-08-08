package org.silkframework.plugins.filter

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.plugins.transformer.filter.RemoveStopWords
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "removeDefaultStopWords",
  categories = Array("Filter"),
  label = "Remove default stop words",
  description = "Removes default stop words from all values.",
  documentationFile = "RemoveDefaultStopWordsTransformer.md"
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
case class RemoveDefaultStopWordsTransformer() extends RemoveStopWords()
