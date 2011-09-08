package de.fuberlin.wiwiss.silk.config

import xml.Node

/**
 * The configuration of the blocking feature.
 *
 * @param isEnabled Enable/disable blocking.
 * @param blocks The number of blocks. Keeping the default is recommended.
 */
case class Blocking(isEnabled: Boolean = true, blocks: Int = Blocking.DefaultBlocks) {
  require(blocks > 0, "blocks > 0")

  /**
   * Returns the number of blocks if blocking is enabled or 1 if blocking is disabled.
   */
  def enabledBlocks = if(isEnabled) blocks else 1

  def toXML: Node = {
    <Blocking enabled={isEnabled.toString} blocks={blocks.toString}/>
  }
}

object Blocking {
  val DefaultBlocks = 101

  def fromXML(node: Node): Blocking = {
    val enabled = (node \ "@enabled").headOption.map(_.text.toBoolean) match {
      case Some(e) => e
      case None => true
    }

    val blocks = (node \ "@blocks").headOption.map(_.text.toInt).getOrElse(DefaultBlocks)

    Blocking(enabled, blocks)
  }
}
