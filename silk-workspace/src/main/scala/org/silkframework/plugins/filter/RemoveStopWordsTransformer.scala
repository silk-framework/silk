package org.silkframework.plugins.filter

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.plugins.transformer.filter.RemoveStopWords
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.resource.Resource

@Plugin(
  id = "removeStopWords",
  categories = Array("Filter"),
  label = "Remove stop words",
  description = "Removes stop words based on a stop word list resource.",
  documentationFile = "RemoveStopWordsTransformer.md"
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("To be or not to be", "that is the question"),
    output = Array("", "question")
  ),
  new TransformExample(
    input1 = Array("It always seems impossible", "until it's done"),
    output = Array("impossible", "")
  )
))
case class RemoveStopWordsTransformer(@Param(value = "Resource for the stop word list") stopWordList: Resource,
                                      @Param(value = "RegEx for detecting words") separator: String = "[\\s-]+")
  extends RemoveStopWords(separator, stopWordList.loadAsString().split("\n").toSet) {

  override def referencedResources: Seq[Resource] = Seq(stopWordList)
}
