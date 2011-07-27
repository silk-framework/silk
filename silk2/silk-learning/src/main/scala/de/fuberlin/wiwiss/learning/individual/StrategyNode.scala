package de.fuberlin.wiwiss.silk.workbench.learning.tree

import de.fuberlin.wiwiss.silk.util.strategy.{Factory, Strategy}

case class StrategyNode[T <: Strategy](strategy: String, parameters: List[ParameterNode], factory: Factory[T]) extends Node {
  def build() = {
    factory(strategy, parameters.map(p => (p.key, p.value)).toMap)
  }
}

object StrategyNode {
  def load[T <: Strategy](strategy: T, factory: Factory[T]) = factory.unapply(strategy) match {
    case Some((id, parameters)) => StrategyNode(id, parameters.map {
      case (key, value) => ParameterNode(key, value)
    }.toList, factory)
    case None => throw new IllegalArgumentException()
  }
}