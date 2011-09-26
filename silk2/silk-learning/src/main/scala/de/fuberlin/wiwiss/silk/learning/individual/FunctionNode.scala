package de.fuberlin.wiwiss.silk.learning.individual

import de.fuberlin.wiwiss.silk.util.plugin.{PluginFactory, AnyPlugin}

case class FunctionNode[T <: AnyPlugin](id: String, parameters: List[ParameterNode], factory: PluginFactory[T]) extends Node {
  def build() = {
    factory(id, parameters.map(p => (p.key, p.value)).toMap)
  }
}

object FunctionNode {
  def load[T <: AnyPlugin](plugin: T, factory: PluginFactory[T]) = factory.unapply(plugin) match {
    case Some((id, parameters)) => FunctionNode(id, parameters.map {
      case (key, value) => ParameterNode(key, value)
    }.toList, factory)
    case None => throw new IllegalArgumentException()
  }
}