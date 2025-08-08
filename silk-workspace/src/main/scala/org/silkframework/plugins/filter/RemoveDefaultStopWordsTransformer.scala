package org.silkframework.plugins.filter

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.plugins.transformer.filter.RemoveStopWords
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "removeDefaultStopWords",
  categories = Array("Filter"),
  label = "Remove default stop words",
  description = "Removes default stop words from all values." +
    "This filter uses the following list of English stop words as a default:" +
    " https://gist.githubusercontent.com/rg089/35e00abf8941d72d419224cfd5b5925d/raw/12d899b70156fd0041fa9778d657330b024b959c/stopwords.txt" +
    " Should you want to provide an own stop word list, either as a resource (e.g. a file) or a remote URL," +
    " see the filters 'removeStopWords' and 'removeRemoteStopWords'."
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
