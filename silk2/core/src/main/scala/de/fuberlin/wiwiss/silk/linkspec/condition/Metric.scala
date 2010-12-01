package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.util.{Strategy, Factory}

trait Metric extends Strategy
{
  def evaluate(value1 : String, value2 : String) : Double

  def index(value : String) : Set[Seq[Int]] = Set(Seq(0))

  val blockCounts : Seq[Int] = Seq(1)
}

object Metric extends Factory[Metric]
