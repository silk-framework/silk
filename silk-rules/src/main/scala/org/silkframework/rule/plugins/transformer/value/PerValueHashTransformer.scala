package org.silkframework.rule.plugins.transformer.value

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}

import java.nio.charset.StandardCharsets
import java.security.{MessageDigest, NoSuchAlgorithmException}

/**
 * Hashes each input value independently and returns one hash per value, preserving cardinality.
 *
 * Accepts exactly one input port. Throws [[IllegalArgumentException]] if more than one port is connected.
 * Each value is encoded as UTF-8 and hashed using a fresh [[java.security.MessageDigest]] instance.
 * The output is a lowercase hexadecimal string for each input value, in the same order as the input.
 *
 * @see [[InputHashTransformer]] for the combining variant that produces a single hash for all input values combined.
 */
@Plugin(
  id = PerValueHashTransformer.pluginId,
  categories = Array("Value"),
  label = "Per-value hash",
  description = """Hashes each input value independently and returns one hash per value. Accepts exactly one input port.""",
  // TODO: documentationFile = "PerValueHashTransformer.md",
  relatedPlugins = Array(
    new PluginReference(
      id = InputHashTransformer.pluginId,
      description = "The Combined input hash plugin produces one combined hash for all input values. The Per-value hash plugin instead hashes each value independently, preserving cardinality."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    description = "Single value produces one SHA-256 hash.",
    input1 = Array("input value"),
    output = Array("f708c2afff0ed197e8551c4dd549ee5b848e0b407106cbdb8e451c8cd1479362")
  ),
  new TransformExample(
    description = "Two values in, two independent hashes out — one per value, not a combined hash.",
    input1 = Array("apple", "banana"),
    output = Array("3a7bd3e2360a3d29eea436fcfb7e44c735d117c42d1c1835420b6b9942dd4f1b",
                   "b493d48364afe44d11c0165cf470a4164d1e2609911ef998be868d46ade3de4e")
  ),
  new TransformExample(
    description = "The algorithm parameter selects the hash function (MD5), single value.",
    parameters = Array("algorithm", "MD5"),
    input1 = Array("apple"),
    output = Array("1f3870be274f6c49b3e31a0c6728957f")
  ),
  new TransformExample(
    description = "The algorithm parameter selects the hash function (MD5), multiple values.",
    parameters = Array("algorithm", "MD5"),
    input1 = Array("apple", "banana"),
    output = Array("1f3870be274f6c49b3e31a0c6728957f", "72b302bf297a228a75730123efef7c41")
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
    description = "Empty input produces empty output.",
    input1 = Array(),
    output = Array()
  ),
  new TransformExample(
    description = "Two input ports causes IllegalArgumentException.",
    input1 = Array("foo"),
    input2 = Array("bar"),
    throwsException = classOf[IllegalArgumentException]
  ),
  new TransformExample(
    description = "Invalid algorithm name causes NoSuchAlgorithmException.",
    parameters = Array("algorithm", "NONEXISTENT"),
    input1 = Array("foo"),
    throwsException = classOf[NoSuchAlgorithmException]
  )
))
case class PerValueHashTransformer(
  @Param(
    value = "The hash algorithm to be used.",
    autoCompletionProvider = classOf[HashAlgorithmAutoCompletionProvider],
    allowOnlyAutoCompletedValues = true
  )
  algorithm: String = "SHA256") extends Transformer {

  require(algorithm.trim.nonEmpty, "Algorithm must not be empty. Please specify an algorithm, such as 'SHA256'.")

  private def toHex(bytes: Array[Byte]): String =
    bytes.map("%02x".format(_)).mkString

  private def hashValue(v: String): String = {
    val digest = MessageDigest.getInstance(algorithm)
    digest.update(v.getBytes(StandardCharsets.UTF_8))
    toHex(digest.digest())
  }

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    require(values.size == 1,
      s"Per-value hash accepts exactly one input source, but ${values.size} were provided.")
    values.head.map(hashValue)
  }
}

object PerValueHashTransformer {
  final val pluginId = "perValueHash"
}
