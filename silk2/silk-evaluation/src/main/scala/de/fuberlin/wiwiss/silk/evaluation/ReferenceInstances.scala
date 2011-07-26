package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.output.Link

case class ReferenceInstances(positive: Map[Link, SourceTargetPair[Instance]] = Map.empty,
                              negative: Map[Link, SourceTargetPair[Instance]] = Map.empty) {
  def withPositive(instancePair: SourceTargetPair[Instance]) = {
    copy(positive = positive + (new Link(instancePair.source.uri, instancePair.target.uri) -> instancePair))
  }

  def withNegative(instancePair: SourceTargetPair[Instance]) = {
    copy(negative = negative + (new Link(instancePair.source.uri, instancePair.target.uri) -> instancePair))
  }
}

object ReferenceInstances {
  def empty = ReferenceInstances(Map.empty, Map.empty)

  def fromInstances(positiveInstances: Traversable[SourceTargetPair[Instance]], negativeInstances: Traversable[SourceTargetPair[Instance]]) = {
    ReferenceInstances(
      positive = positiveInstances.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap,
      negative = negativeInstances.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap
    )
  }
}