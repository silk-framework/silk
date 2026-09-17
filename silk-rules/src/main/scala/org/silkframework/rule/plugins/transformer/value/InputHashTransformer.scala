package org.silkframework.rule.plugins.transformer.value

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.plugins.transformer.combine.ConcatMultipleValuesTransformer
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.rule.plugins.transformer.combine.ZipTransformer
import org.silkframework.rule.plugins.transformer.replace.MapTransformerWithDefaultInput
import org.silkframework.rule.plugins.transformer.value.InputHashTransformer.SeqIntersperseOps
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}

import java.security.MessageDigest

/**
 * Combines all input values across all connected ports into a single hash.
 *
 * Values are fed sequentially into a single [[java.security.MessageDigest]] instance — port 1 first, then port 2, and
 * so on; within each port, values are processed in order. An optional separator, configured via the glue parameter, is
 * inserted between every pair of adjacent values; by default, no separator is used. The output is always exactly one
 * lowercase hexadecimal string, regardless of how many values or ports are provided.
 *
 * @see [[PerValueHashTransformer]] for the per-value variant that produces one hash per input value.
 */
@Plugin(
  id = InputHashTransformer.pluginId,
  categories = Array("Value"),
  label = "Combined input hash",
  description = "Calculates a single hash value covering all input values combined, across all input ports. Values " +
    "are fed into the hash function in port order, with an optional separator (the glue parameter) inserted " +
    "between them.",
  documentationFile = "InputHashTransformer.md",
  relatedPlugins = Array(
    new PluginReference(
      id = PerValueHashTransformer.pluginId,
      description = "The Per-value hash plugin hashes each input value independently and returns one hash per " +
        "value, preserving cardinality. The Combined input hash plugin instead feeds all values into a single hash " +
        "function, producing one combined hash regardless of input size."
    ),
    new PluginReference(
      id = MapTransformerWithDefaultInput.pluginId,
      description = "One hash value is produced for the entire set of inputs by the Combined input hash plugin. The " +
        "Map with default plugin instead keeps a value sequence and rewrites it position by position through the " +
        "mapping, falling back to the second input where no mapping entry is found."
    ),
    new PluginReference(
      id = ConcatTransformer.pluginId,
      description = "Combined input hash always returns exactly one value, however many values or ports are " +
        "connected. Concatenate instead produces every combination of connected values as a separate output, so " +
        "more than one value on an input produces multiple outputs instead of one."
    ),
    new PluginReference(
      id = ZipTransformer.pluginId,
      description = "Combined input hash accepts any number of ports and always returns exactly one value. Zip " +
        "instead requires exactly two inputs, pairs their values by position, and returns one output per position " +
        "rather than one combined result."
    ),
    new PluginReference(
      id = ConcatMultipleValuesTransformer.pluginId,
      description = "Concatenate multiple values combines everything received on one port into a single string, " +
        "but keeps each port's result separate; more ports still return separate results, one per port. Combined " +
        "input hash instead crosses every port boundary, always returning one hash value overall."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    description = "A single input value produces one combined SHA-256 hash.",
    input1 = Array("input value"),
    output = Array("f708c2afff0ed197e8551c4dd549ee5b848e0b407106cbdb8e451c8cd1479362")
  ),
  new TransformExample(
    description = "Multiple values on one input are combined into a single hash.",
    input1 = Array("apple", "banana"),
    output = Array("5b692305517af54eb5ae12b9ff89eaf89e31f6a6ee208365886a18b81a2fc2f8")
  ),
  new TransformExample(
    description = "The order in which values are combined affects the resulting hash.",
    input1 = Array("banana", "apple"),
    output = Array("d4183362b538440bb9a5f82359791c647280e6b657a1812f16f7bcc2b8f141ca")
  ),
  new TransformExample(
    description = "Values from multiple ports are combined in port order: port 1's values first, then port 2's.",
    input1 = Array("apple"),
    input2 = Array("banana"),
    output = Array("5b692305517af54eb5ae12b9ff89eaf89e31f6a6ee208365886a18b81a2fc2f8")
  ),
  new TransformExample(
    description = "The glue parameter inserts a separator between values.",
    parameters = Array("glue", "-"),
    input1 = Array("apple", "banana"),
    output = Array("d6300f978b8699862450cbabc35349239c09e95c356723c32fc1407ec8104c78")
  ),
  new TransformExample(
    description = "Values from multiple ports are combined using the glue separator in port order: port 1's values " +
      "first, then port 2's.",
    parameters = Array("glue", "-"),
    input1 = Array("apple"),
    input2 = Array("banana"),
    output = Array("d6300f978b8699862450cbabc35349239c09e95c356723c32fc1407ec8104c78")
  ),
  new TransformExample(
    description = "The glue parameter accepts escaped characters, here a newline.",
    parameters = Array("glue", "\\n"),
    input1 = Array("apple", "banana"),
    output = Array("5c573825e0a168e9e382f9b7e78e6603271634dceac792b9c92fa77a27d404f8")
  ),
  new TransformExample(
    description = "Values containing non-ASCII characters are encoded as UTF-8 before being hashed.",
    parameters = Array("glue", "-"),
    input1 = Array("café", "banana"),
    output = Array("c17ffffc64b5ffd83dbc2a975134c65a7f7f457a898b6e1fc6d2db1217fa92ff")
  ),
  new TransformExample(
    description = "The glue parameter accepts a separator longer than one character.",
    parameters = Array("glue", "::"),
    input1 = Array("apple", "banana"),
    output = Array("7f721cdbf970bad79a82e7d827e0e5eef3d8263610aa6f062427060f5d2379db")
  ),
  new TransformExample(
    description = "The order of values combined with a glue separator affects the resulting hash.",
    parameters = Array("glue", "-"),
    input1 = Array("banana", "apple"),
    output = Array("4472ed905ebe09b0e7a37ffa5adc6b0b2f052a6d0045fc037e2e56cdb45fba0a")
  ),
  new TransformExample(
    description = "The glue separator is never applied to a single value, since there is no adjacent value to " +
      "separate it from.",
    parameters = Array("glue", "-"),
    input1 = Array("solo"),
    output = Array("5364f2f2fc4f54e9d47ad29cfb08ef430c8153394bf2a0dff5cbe77a0ffef861")
  ),
  new TransformExample(
    description = "With more than two values, the glue separator is inserted between every adjacent pair.",
    parameters = Array("glue", "-"),
    input1 = Array("a", "b"),
    input2 = Array("c"),
    output = Array("cbd2be7b96f770a0326948ebd158cf539fab0627e8adbddc97f7a65c6a8ae59a")
  ),
  new TransformExample(
    description = "Three values in one port produce a hash with the glue separator inserted between every pair.",
    parameters = Array("glue", "-"),
    input1 = Array("a", "b", "c"),
    output = Array("cbd2be7b96f770a0326948ebd158cf539fab0627e8adbddc97f7a65c6a8ae59a")
  ),
  new TransformExample(
    description = "A value that is identical to the glue string is hashed as ordinary byte content, with no " +
      "special escaping.",
    parameters = Array("glue", "-"),
    input1 = Array("a", "-", "b"),
    output = Array("1acd27f89b4f44ddaf3b81fadb5491ea61f9ae3981545cf62d3502a387bcf350")
  ),
  new TransformExample(
    description = "Values from three ports are combined using the glue separator in port order: port 1's value, " +
      "then port 2's, then port 3's.",
    parameters = Array("glue", "-"),
    input1 = Array("a"),
    input2 = Array("b"),
    input3 = Array("c"),
    output = Array("cbd2be7b96f770a0326948ebd158cf539fab0627e8adbddc97f7a65c6a8ae59a")
  ),
  new TransformExample(
    description = "The order in which values are combined affects the resulting hash, even with more than two " +
      "values and a glue separator.",
    parameters = Array("glue", "-"),
    input1 = Array("c", "b"),
    input2 = Array("a"),
    output = Array("8e599734c8f75bd7866757e5f9cd4ab3cc173d753fb858a85ae6efb817e3e200")
  ),
  new TransformExample(
    description = "With more than two values, the SHA-1 algorithm still inserts the glue separator between every " +
      "adjacent pair.",
    parameters = Array("algorithm", "SHA-1", "glue", "-"),
    input1 = Array("a", "b"),
    input2 = Array("c"),
    output = Array("e088d9e3f737c091378fe8494936b16d51eb42ee")
  ),
  new TransformExample(
    description = "With the SHA-512 algorithm selected, the glue separator is inserted between every adjacent " +
      "pair of three values.",
    parameters = Array("algorithm", "SHA-512", "glue", "-"),
    input1 = Array("a", "b"),
    input2 = Array("c"),
    output = Array("a271a54e93179f7dccb132a17c914e66e76a91ae3b7cb8b3abffa21a8ffb66b65a2883769bc4c6f95ab39fbd039cfe21046b485cef1588269d2605c7253f2b98")
  ),
  new TransformExample(
    description = "With the MD5 algorithm selected, the glue parameter still inserts a separator between values.",
    parameters = Array("algorithm", "MD5", "glue", "-"),
    input1 = Array("apple", "banana"),
    output = Array("9b22c402a5166c71ebdf2d1b99f37727")
  ),
  new TransformExample(
    description = "With the MD5 algorithm selected, escaped glue characters are still converted before being used " +
      "as the separator.",
    parameters = Array("algorithm", "MD5", "glue", "\\n"),
    input1 = Array("apple", "banana"),
    output = Array("cf0bf238cadd6688db6f8d2511a52ebf")
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
  new TransformExample(
    description = "Empty algorithm string causes IllegalArgumentException.",
    parameters = Array("algorithm", ""),
    input1 = Array("foo"),
    throwsException = classOf[IllegalArgumentException]
  ),
))
case class InputHashTransformer(
  @Param(
    value = "The hash algorithm to be used.",
    autoCompletionProvider = classOf[HashAlgorithmAutoCompletionProvider],
    allowOnlyAutoCompletedValues = true
  )
  algorithm: String = "SHA256",
  @Param(ConcatTransformer.glueDescription)
  glue: String = "",
) extends HashTransformer {

  require(algorithm.trim.nonEmpty, "Algorithm must not be empty. Please specify an algorithm, such as 'SHA256'.")

  // glue with escaped char sequences (\\, \n, \t) converted to actual character.
  lazy val parsedGlue: String = ConcatTransformer.parseGlue(glue)

  /** Updates the digest with the UTF-8 encoding of all values. */
  private def updateAll(digest: MessageDigest)(values: Seq[Seq[String]], separator: String): Unit =
    values.flatten.intersperse(separator).foreach(updateWith(digest, _))

  /** Hashes several values into a single output */
  private def hashValues(values: Seq[Seq[String]], separator: String): String =
    withDigest { digest =>
      updateAll(digest)(values, separator)
      toHex(digest.digest())
    }

  override def apply(values: Seq[Seq[String]]): Seq[String] = Seq(hashValues(values, parsedGlue))
}

object InputHashTransformer {
  final val pluginId = "inputHash"

  implicit class SeqIntersperseOps[A](val underlying: Seq[A]) extends AnyVal {
    def intersperse[B >: A](separator: B): Seq[B] = underlying.toList match {
      case Nil => Nil
      case head :: tail => head :: tail.foldRight(List.empty[B])((element, acc) => separator :: element :: acc)
    }
  }
}
