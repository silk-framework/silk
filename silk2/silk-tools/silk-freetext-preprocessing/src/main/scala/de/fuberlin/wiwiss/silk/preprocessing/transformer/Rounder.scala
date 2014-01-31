package de.fuberlin.wiwiss.silk.preprocessing.transformer

/**
 * Created with IntelliJ IDEA.
 * User: Petar
 * Date: 21/01/14
 * Time: 14:06
 * To change this template use File | Settings | File Templates.
 */
case class Rounder(){
  private[this] val compiledRegex = "^[+-]?\\d+(\\.\\d+)?$".r
  private val empty = ""

  def checkRound(s: String): String = {
    s match {
      case compiledRegex(s) => s.toDouble.floor.toInt.toString
    }
  }

  def apply(values:Set[String]):Set[String] = {
       values.map(x=>checkRound(x))
  }

}
