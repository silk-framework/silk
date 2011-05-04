package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.output.Link

case class ReferenceInstances(positive : Map[Link, SourceTargetPair[Instance]] = Map.empty,
                              negative : Map[Link, SourceTargetPair[Instance]] = Map.empty)

object ReferenceInstances
{
  def empty = ReferenceInstances(Map.empty, Map.empty)

  def fromInstances(positiveInstances : Traversable[SourceTargetPair[Instance]], negativeInstances : Traversable[SourceTargetPair[Instance]]) =
  {
    ReferenceInstances(
      positive = positiveInstances.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap,
      negative = negativeInstances.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap
    )
  }
}