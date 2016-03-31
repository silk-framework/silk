package org.silkframework.plugins.transformer.value

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.{Param, Plugin}

import scala.util.Random

@Plugin(
   id = "randomNumber",
   label = "Random Number",
   categories = Array("Value"),
   description = "Generates a set of random numbers."
 )
case class RandomNumberTransformer(
  @Param("The smallest number that could be generated.")
  min: Double = 0.0,
  @Param("The largest number that could be generated.")
  max: Double = 100.0,
  @Param("The minimum number of values to generate in each set.")
  minCount: Int = 1,
  @Param("The maximum number of values to generate in each set.")
  maxCount: Int = 1) extends Transformer {

   override def apply(values: Seq[Seq[String]]): Seq[String] = {
     val count = minCount + Random.nextInt(1 + maxCount - minCount)
     Seq.fill(count)(min + (max - min) * Random.nextDouble()).map(_.toString)
   }
 }