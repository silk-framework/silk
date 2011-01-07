package de.fuberlin.wiwiss.silk.util.strategy

/**
 * Thrown if a strategy is invalid.
 */
class InvalidStrategyException(e : String, cause : Throwable) extends Exception(e)
{
  def this(e : String) = this(e, null)
}