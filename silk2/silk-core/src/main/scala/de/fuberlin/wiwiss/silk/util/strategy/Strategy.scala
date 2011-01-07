package de.fuberlin.wiwiss.silk.util.strategy

/**
 * A strategy which can have different implementations.
 */

trait Strategy
{
  private[strategy] var id = ""
  private[strategy] var parameters = Map[String, String]()
}
