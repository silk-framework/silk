package de.fuberlin.wiwiss.silk.util

import scala.collection.mutable.HashMap

/**
 * An abstract Factory.
 */
class Factory[T <: Strategy] extends Function2[String, Map[String, String], T]
{
  private val classes = HashMap[String, StrategyDefinition]()

  def register[U <: T](name : String, implementationClass : Class[U], defaultParams : Map[String, String] = Map.empty)
  {
    //TODO check if implementationClass provides a constructor of the type (Map[String, String])
    classes.update(name, new StrategyDefinition(implementationClass, defaultParams))
  }

  def apply(name : String, params : Map[String, String] = Map.empty) : T =
  {
    classes.get(name) match
    {
      case Some(strategy) => strategy.clazz.getConstructor(classOf[Map[String, String]])
          .newInstance(strategy.params ++ params)
          .asInstanceOf[T]
      case None => throw new IllegalArgumentException("No implementation found with name " + name)
    }
  }

  def unapply(t : T) : Option[(String, Map[String, String])]  =
  {
    classes.find { case (name, strategy) => strategy.clazz.isAssignableFrom(t.getClass) } match
    {
      case Some((name, strategy)) => Some((name, t.params))
      case None => None
    }
  }

  def availableStrategies : Traversable[(String, String)] = classes.iterator.toList.map{case (id, strategy) => (id, strategy.clazz.getSimpleName)}

  private class StrategyDefinition(val clazz : Class[_], val params : Map[String, String])
}
