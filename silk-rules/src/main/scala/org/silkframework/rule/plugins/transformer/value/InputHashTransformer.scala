package org.silkframework.rule.plugins.transformer.value

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.plugins.transformer.replace.MapTransformerWithDefaultInput
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}
import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider}
import org.silkframework.workspace.WorkspaceReadTrait

import java.nio.charset.StandardCharsets
import java.security.{MessageDigest, Security}
import scala.jdk.CollectionConverters.IterableHasAsScala

@Plugin(
  id = InputHashTransformer.pluginId,
  categories = Array("Value"),
  label = "Input hash",
  description = """Calculates the hash sum of the input values. Generates a single hash sum for all input values combined.""",
  documentationFile = "InputHashTransformer.md",
  relatedPlugins = Array(
    new PluginReference(
      id = MapTransformerWithDefaultInput.pluginId,
      description = "One hash value is produced for the entire set of inputs by the Input hash plugin. The Map with default plugin instead keeps a value sequence and rewrites it position by position through the mapping, falling back to the second input where no mapping entry is found."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("input value"),
    output = Array("f708c2afff0ed197e8551c4dd549ee5b848e0b407106cbdb8e451c8cd1479362")
  ),
))
case class InputHashTransformer(@Param(value = "The hash algorithm to be used.",
                                      autoCompletionProvider = classOf[HashAlgorithmAutoCompletionProvider], allowOnlyAutoCompletedValues = true)
                                algorithm: String = "SHA256") extends Transformer {

  require(algorithm.trim.nonEmpty, "Algorithm must not be empty. Please specify an algorithm, such as 'SHA256'.")

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    val hashSum = MessageDigest.getInstance(algorithm)
    for(value <- values; v <- value) {
      hashSum.update(v.getBytes(StandardCharsets.UTF_8))
    }
    // Convert the byte array to a hexadecimal string
    Seq(hashSum.digest().map("%02x".format(_)).mkString)
  }
}

object InputHashTransformer {
  final val pluginId = "inputHash"
}

/**
 * Auto-completion for project resources.
 */
case class HashAlgorithmAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {

  private lazy val algorithms = {
    Security.getAlgorithms("MessageDigest").asScala.toSeq
  }

  override def autoComplete(searchQuery: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Iterable[AutoCompletionResult] = {

    val multiSearchWords = extractSearchTerms(searchQuery)
    algorithms
      .filter(r => matchesSearchTerm(multiSearchWords, r.toLowerCase))
      .map(r => AutoCompletionResult(r, None))
  }

  override def valueToLabel(value: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = {
    None
  }
}
