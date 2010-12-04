package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance

trait Operator
{
  val required : Boolean

  val weight : Int

  def apply(sourceInstance : Instance, targetInstance : Instance, threshold : Double) : Option[Double]

  def index(instance : Instance, threshold : Double) : Set[Seq[Int]]

  val blockCounts : Seq[Int]
}
