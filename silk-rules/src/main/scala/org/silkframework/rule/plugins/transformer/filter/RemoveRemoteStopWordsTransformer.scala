package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.plugins.transformer.replace.RegexReplaceTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}

import scala.io.Source

@Plugin(
  id = RemoveRemoteStopWordsTransformer.pluginId,
  categories = Array("Filter"),
  label = "Remove remote stop words",
  description = "Removes stop words based on a stop word list remote URL.",
  documentationFile = "RemoveRemoteStopWordsTransformer.md",
  relatedPlugins = Array(
    new PluginReference(
      id = RegexReplaceTransformer.pluginId,
      description = "The Remove remote stop words plugin removes tokens case-insensitively based on a stop word list loaded from a remote URL after splitting the input with the separator regex. The Regex replace plugin rewrites or deletes substrings based on a regular expression match in the string, which fits cases where the noise is defined by pattern rather than by a word list."
    )
  )
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
case class RemoveRemoteStopWordsTransformer(@Param(value = "URL of the stop word list") stopWordListUrl: String = RemoveRemoteStopWordsTransformer.defaultStopWordListUrl,
                                            @Param(value = "RegEx for detecting words") separator: String = "[\\s-]+")
  extends RemoveStopWords(separator, RemoveRemoteStopWordsTransformer.loadStopWords(stopWordListUrl))

object RemoveRemoteStopWordsTransformer {
  final val pluginId = "removeRemoteStopWords"

  private val defaultStopWordListUrl = "https://raw.githubusercontent.com/stopwords-iso/stopwords-en/refs/heads/master/stopwords-en.txt"

  private def loadStopWords(stopWordListUrl: String): Set[String] = {
    val html = Source.fromURL(stopWordListUrl)
    try {
      html.getLines().toSet
    } finally {
      html.close()
    }
  }
}
