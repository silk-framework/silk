package org.silkframework.rule.plugins.transformer.value

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider}
import org.silkframework.workspace.WorkspaceReadTrait

import java.nio.charset.StandardCharsets
import java.security.{MessageDigest, Security}
import scala.jdk.CollectionConverters.IterableHasAsScala

@Plugin(
  id = "inputHash",
  categories = Array("Value"),
  label = "Input hash",
  description =
"""Calculates the hash sum of the input values.
This task supports using different hash algorithms from the [Secure Hash Algorithms family|https://en.wikipedia.org/wiki/Secure_Hash_Algorithms] (SHA, e.g. SHA256) and two algorithms from the [Message-Digest Algorithm family|https://en.wikipedia.org/wiki/MD5] (MD2 / MD5). Please be aware that some of these algorithms are not secure regarding collision- and other attacks."""
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

