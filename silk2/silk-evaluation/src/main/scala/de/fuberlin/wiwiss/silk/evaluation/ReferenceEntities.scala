package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.output.Link

case class ReferenceEntities(positive: Map[Link, SourceTargetPair[Entity]] = Map.empty,
                              negative: Map[Link, SourceTargetPair[Entity]] = Map.empty) {
  def withPositive(entityPair: SourceTargetPair[Entity]) = {
    copy(positive = positive + (new Link(entityPair.source.uri, entityPair.target.uri) -> entityPair))
  }

  def withNegative(entityPair: SourceTargetPair[Entity]) = {
    copy(negative = negative + (new Link(entityPair.source.uri, entityPair.target.uri) -> entityPair))
  }
}

object ReferenceEntities {
  def empty = ReferenceEntities(Map.empty, Map.empty)

  def fromEntities(positiveEntities: Traversable[SourceTargetPair[Entity]], negativeEntities: Traversable[SourceTargetPair[Entity]]) = {
    ReferenceEntities(
      positive = positiveEntities.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap,
      negative = negativeEntities.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap
    )
  }
}