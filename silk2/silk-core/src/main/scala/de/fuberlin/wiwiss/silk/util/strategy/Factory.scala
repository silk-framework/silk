package de.fuberlin.wiwiss.silk.util.strategy

/**
 * An abstract Factory.
 */
class Factory[T <: Strategy] extends ((String, Map[String, String]) => T)
{
  private var strategies = Map[String, StrategyDefinition[T]]()

  override def apply(id : String, params : Map[String, String] = Map.empty) : T =
  {
    val strategy =
    {
      strategies.get(id) match
      {
        case Some(strategy) => strategy(params)
        case None => throw new NoSuchElementException("No strategy called '" + id + "' found.")
      }
    }

    strategy.id = id
    strategy.parameters = params

    strategy
  }

  def unapply(t : T) : Option[(String, Map[String, String])]  =
  {
    Some(t.id, t.parameters)
  }

  def register[U <: T](implementationClass : Class[U])
  {
    val strategyDefinition = StrategyDefinition(implementationClass)

    strategies += ((strategyDefinition.id, strategyDefinition))
  }

  def availableStrategies : Traversable[StrategyDefinition[T]] = strategies.values
}
