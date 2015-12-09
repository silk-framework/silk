package org.silkframework.plugins.transformer.value

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.Plugin

import scala.util.Random

@Plugin(
   id = "randomNumber",
   label = "Random Number",
   categories = Array("Value"),
   description = "Generates a set of random numbers."
 )
case class RandomNumberTransformer(min: Double = 0.0, max: Double = 100.0, minCount: Int = 1, maxCount: Int = 1) extends Transformer {
   override def apply(values: Seq[Seq[String]]): Seq[String] = {
     val count = minCount + Random.nextInt(1 + maxCount - minCount)
     Seq.fill(count)(min + (max - min) * Random.nextDouble()).map(_.toString)
   }
 }