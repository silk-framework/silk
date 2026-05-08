package org.silkframework.rule.plugins.transformer.value

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.plugins.transformer.replace.MapTransformerWithDefaultInput
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Combines all input values across all connected ports into a single hash.
 *
 * Values are fed sequentially into a single [[java.security.MessageDigest]] instance — port 1 first, then port 2,
 * and so on; within each port, values are processed in order. No separator is inserted between values or ports.
 * The output is always exactly one lowercase hexadecimal string, regardless of how many values or ports are provided.
 *
 * @see [[PerValueHashTransformer]] for the per-value variant that produces one hash per input value.
 */
@Plugin(
  id = InputHashTransformer.pluginId,
  categories = Array("Value"),
  label = "Combined input hash",
  description = """Calculates a single hash value covering all input values combined, across all input ports. Values are fed into the hash function in port order without any separator between them.""",
  documentationFile = "InputHashTransformer.md",
  relatedPlugins = Array(
    new PluginReference(
      id = MapTransformerWithDefaultInput.pluginId,
      description = "One hash value is produced for the entire set of inputs by the Combined input hash plugin. The Map with default plugin instead keeps a value sequence and rewrites it position by position through the mapping, falling back to the second input where no mapping entry is found."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    description = "One SHA-256 hash per input value.",
    input1 = Array("input value"),
    output = Array("f708c2afff0ed197e8551c4dd549ee5b848e0b407106cbdb8e451c8cd1479362")
  ),
  new TransformExample(
    description = "Multiple values on one input are combined into a single hash.",
    input1 = Array("apple", "banana"),
    output = Array("5b692305517af54eb5ae12b9ff89eaf89e31f6a6ee208365886a18b81a2fc2f8")
  ),
  new TransformExample(
    description = "Reversing the value order produces a different hash, confirming order-sensitivity.",
    input1 = Array("banana", "apple"),
    output = Array("d4183362b538440bb9a5f82359791c647280e6b657a1812f16f7bcc2b8f141ca")
  ),
  new TransformExample(
    description = "Values from multiple ports are combined in port order, producing the same hash as the equivalent single-port sequence.",
    input1 = Array("apple"),
    input2 = Array("banana"),
    output = Array("5b692305517af54eb5ae12b9ff89eaf89e31f6a6ee208365886a18b81a2fc2f8")
  ),
  new TransformExample(
    description = "The algorithm parameter selects the hash function (MD5).",
    parameters = Array("algorithm", "MD5"),
    input1 = Array("input value"),
    output = Array("cee963a28f70ee97751a85ef732e66dd")
  ),
  new TransformExample(
    description = "The algorithm parameter selects the hash function (SHA-1).",
    parameters = Array("algorithm", "SHA-1"),
    input1 = Array("apple"),
    output = Array("d0be2dc421be4fcd0172e5afceea3970e2f3d940")
  ),
  new TransformExample(
    description = "The algorithm parameter selects the hash function (SHA-384).",
    parameters = Array("algorithm", "SHA-384"),
    input1 = Array("apple"),
    output = Array("3d8786fcb588c93348756c6429717dc6c374a14f7029362281a3b21dc10250ddf0d0578052749822eb08bc0dc1e68b0f")
  ),
  new TransformExample(
    description = "The algorithm parameter selects the hash function (SHA-512).",
    parameters = Array("algorithm", "SHA-512"),
    input1 = Array("apple"),
    output = Array("844d8779103b94c18f4aa4cc0c3b4474058580a991fba85d3ca698a0bc9e52c5940feb7a65a3a290e17e6b23ee943ecc4f73e7490327245b4fe5d5efb590feb2")
  ),
  new TransformExample(
    description = "Empty input produces the hash of an empty message.",
    input1 = Array(),
    output = Array("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
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
