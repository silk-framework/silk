package de.fuberlin.wiwiss.silk.preprocessing.transformer

/**
 * Ronder
 * Rounds a double to int
 *
 * @param round Takes floor or ceil as function
 */
case class Rounder(round: Double => Double) extends Transformer{
  private[this] val compiledRegex = "^[+-]?\\d+(\\.\\d+)?$".r
  private val empty = ""

  def checkRound(s: String): String = {
    s match {
      case compiledRegex(s) => s.toDouble.round.toInt.toString
    }
  }

  override def apply(values: List[String]): List[String] = {
       values.map(x=>checkRound(x))
  }

}
