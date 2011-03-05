package de.fuberlin.wiwiss.silk.util.strategy

/**
 * A strategy which can have different implementations.
 */
trait Strategy
{
  private[strategy] var id = ""
  private[strategy] var parameters = Map[String, String]()

  override def toString =
  {
    getClass.getSimpleName + "(" + parameters.map{ case(key, value) => key + "=" + value}.mkString + ")"
  }
}
