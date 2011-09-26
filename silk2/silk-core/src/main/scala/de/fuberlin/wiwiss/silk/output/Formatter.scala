package de.fuberlin.wiwiss.silk.output

import de.fuberlin.wiwiss.silk.util.plugin.{PluginFactory, AnyPlugin}

/**
 * Serializes a link.
 */
trait Formatter extends AnyPlugin {
  def header: String = ""

  def footer: String = ""

  def format(link: Link, predicate: String): String
}

/**
 * Formatter factory
 */
object Formatter extends PluginFactory[Formatter]
