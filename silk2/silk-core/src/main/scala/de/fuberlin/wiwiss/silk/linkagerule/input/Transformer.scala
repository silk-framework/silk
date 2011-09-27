package de.fuberlin.wiwiss.silk.linkagerule.input

import de.fuberlin.wiwiss.silk.util.plugin.{PluginFactory, AnyPlugin}

/**
 * Transforms values.
 */
trait Transformer extends AnyPlugin {
  def apply(values: Seq[Set[String]]): Set[String]
}

object Transformer extends PluginFactory[Transformer]
