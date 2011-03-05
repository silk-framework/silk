package de.fuberlin.wiwiss.silk.linkspec.input

import de.fuberlin.wiwiss.silk.util.strategy.{Factory, Strategy}

/**
 * Transforms a value.
 */
trait Transformer extends Strategy
{
  def evaluate(strings : Seq[String]) : String
}

object Transformer extends Factory[Transformer]
