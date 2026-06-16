package org.silkframework.plugins.filter

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.plugins.transformer.filter.{RemoveRemoteStopWordsTransformer, RemoveStopWords}
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
  id = RemoveDefaultStopWordsTransformer.pluginId,
  categories = Array("Filter"),
  label = "Remove default stop words",
  description = "Removes stop words based on a default stop word list.",
  documentationFile = "RemoveDefaultStopWordsTransformer.md",
  relatedPlugins = Array(
    new PluginReference(
      id = RemoveRemoteStopWordsTransformer.pluginId,
      description = "The Remove remote stop words plugin performs stop word removal using a stop word list fetched from a remote URL, while the Remove default stop words plugin performs stop word removal using the built-in default list."
    )
  ),
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

object RemoveDefaultStopWordsTransformer {
  final val pluginId = "removeDefaultStopWords"
}
