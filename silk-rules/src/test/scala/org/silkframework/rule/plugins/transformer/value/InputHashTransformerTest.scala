package org.silkframework.rule.plugins.transformer.value

import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.rule.test.TransformerTest

import scala.util.Random

class InputHashTransformerTest extends TransformerTest[InputHashTransformer] {

  private val algorithms = Seq("SHA256", "MD5", "SHA-1", "SHA-384", "SHA-512")
  private val glues = Seq("", "-", "\\n")

  private def randomString(random: Random, length: Int): String = {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!?.,;: "
    (1 to length).map(_ => chars(random.nextInt(chars.length))).mkString
  }

  private def randomPairs(seed: Long, count: Int): Seq[(String, String)] = {
    val random = new Random(seed)
    (1 to count).map { _ =>
      randomString(random, random.nextInt(41)) -> randomString(random, random.nextInt(41))
    }
  }

  it should "produce the same hash as concatenating first with ConcatTransformer and then hashing the single result" in {
    for {
      (a, b) <- randomPairs(seed = 42L, count = 20)
      algorithm <- algorithms
      glue <- glues
    } {
      val concatenated = ConcatTransformer(glue = glue).apply(Seq(Seq(a), Seq(b))).head
      val hashOfConcatenated = InputHashTransformer(algorithm = algorithm).apply(Seq(Seq(concatenated)))

      val combinedHash = InputHashTransformer(algorithm = algorithm, glue = glue).apply(Seq(Seq(a), Seq(b)))

      combinedHash shouldEqual hashOfConcatenated
    }
  }

  it should "produce the same MD5 hash as concatenating first with ConcatTransformer" in {
    for {
      a <- Seq("", "a", "hello world")
      b <- Seq("b", "banana", "test!")
      glue <- glues
    } {
      val concatenated = ConcatTransformer(glue = glue).apply(Seq(Seq(a), Seq(b))).head
      val hashOfConcatenated = InputHashTransformer(algorithm = "MD5").apply(Seq(Seq(concatenated)))

      val combinedHash = InputHashTransformer(algorithm = "MD5", glue = glue).apply(Seq(Seq(a), Seq(b)))

      combinedHash shouldEqual hashOfConcatenated
    }
  }

  it should "not distinguish a value's own separator character from inserted glue" in {
    val asOneValue = InputHashTransformer().apply(Seq(Seq("grand-mère")))
    val asSplitValuesWithGlue = InputHashTransformer(glue = "-").apply(Seq(Seq("grand", "mère")))

    asSplitValuesWithGlue shouldEqual asOneValue
  }

  it should "produce the same hash for three glued values regardless of how they are split across ports" in {
    val transformer = InputHashTransformer(glue = "-")

    val singlePort = transformer.apply(Seq(Seq("a", "b", "c")))
    val twoPorts = transformer.apply(Seq(Seq("a", "b"), Seq("c")))
    val threePorts = transformer.apply(Seq(Seq("a"), Seq("b"), Seq("c")))

    twoPorts shouldEqual singlePort
    threePorts shouldEqual singlePort
  }
}
