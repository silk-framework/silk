package de.fuberlin.wiwiss.silk.plugins.transformer.value

import de.fuberlin.wiwiss.silk.rule.input.Transformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

import scala.util.Random

@Plugin(
   id = "randomNumber",
   label = "Random Number",
   categories = Array("Value"),
   description = "Generates a set of random numbers."
 )
case class RandomNumberTransformer(min: Double = 0.0, max: Double = 100.0, minCount: Int = 1, maxCount: Int = 1) extends Transformer {
   override def apply(values: Seq[Set[String]]): Set[String] = {
     val count = minCount + Random.nextInt(1 + maxCount - minCount)
     Traversable.fill(count)(min + (max - min) * Random.nextDouble()).map(_.toString).toSet
   }
 }