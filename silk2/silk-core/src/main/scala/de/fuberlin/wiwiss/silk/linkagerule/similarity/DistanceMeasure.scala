package de.fuberlin.wiwiss.silk.linkagerule.similarity

import de.fuberlin.wiwiss.silk.util.plugin.{AnyPlugin, PluginFactory}
import de.fuberlin.wiwiss.silk.entity.Index

trait DistanceMeasure extends AnyPlugin {
  //TODO accept set instead of traversable?
  def apply(values1: Traversable[String], values2: Traversable[String], limit: Double = Double.PositiveInfinity): Double

  def index(values: Set[String], limit: Double): Index = Index.default
}

object DistanceMeasure extends PluginFactory[DistanceMeasure]
