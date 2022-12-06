package org.silkframework.rule.plugins.transformer.normalize

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, TransformExample, TransformExamples}

@Plugin(
  id = "sortWords",
  categories = Array("Normalize"),
  label = "Sort words",
  description = "Sorts all words in each value lexicographically."
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array(),
    output = Array()
  ),
  new TransformExample(
    input1 = Array("c a b"),
    output = Array("a b c")
  ),
  new TransformExample(
    input1 = Array("Hans Hansa    Hamburg", "München Marburg"),
    output = Array("Hamburg Hans Hansa", "Marburg München")
  )
))
case class SortWordsTransformer(@Param("The regular expression used to split values into words.")
                                splitRegex: String = "\\s+",
                                @Param("Separator to be inserted between sorted words.")
                                glue: String = " ") extends SimpleTransformer {

  private val compiledRegex = splitRegex.r

  override def evaluate(value: String): String= {
    val words = compiledRegex.split(value)
    words.sorted.mkString(glue)
  }
}
