package de.fuberlin.wiwiss.silk.linkspec.condition

import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

trait Operator
{
  val required : Boolean

  val weight : Int

  def apply(instances : SourceTargetPair[Instance], threshold : Double) : Option[Double]

  def index(instance : Instance, threshold : Double) : Set[Seq[Int]]

  val blockCounts : Seq[Int]
}
