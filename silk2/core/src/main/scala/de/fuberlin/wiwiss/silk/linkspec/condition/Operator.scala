package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance

trait Operator
{
  val required : Boolean

  val weight : Int

  def apply(sourceInstance : Instance, targetInstance : Instance) : Traversable[Double]

  def index(instance : Instance) : Set[Seq[Int]] = Set(Seq(0))

  val blockCounts : Seq[Int] = Seq(1)
}
