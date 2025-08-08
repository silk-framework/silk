package org.silkframework.plugins.filter

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.plugins.transformer.filter.RemoveStopWords
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.Resource

@Plugin(
  id = "removeStopWords",
  categories = Array("Filter"),
  label = "Remove stop words",
  description = "Removes stop words from all values.",
  documentationFile = "RemoveStopWordsTransformer.md"
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
case class RemoveStopWordsTransformer(stopWordList: Resource, separator: String = "[\\s-]+")
  extends RemoveStopWords(separator, stopWordList.loadAsString().split("\n").toSet) {

  override def referencedResources: Seq[Resource] = Seq(stopWordList)
}
