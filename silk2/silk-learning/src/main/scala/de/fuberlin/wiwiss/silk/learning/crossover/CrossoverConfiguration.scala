package de.fuberlin.wiwiss.silk.learning.crossover

import xml.Node

case class CrossoverConfiguration(operators: Traversable[CrossoverOperator])

object CrossoverConfiguration
{
  def fromXml(xml: Node) = {
    CrossoverConfiguration(
      operators = loadOperators(xml)
    )
  }

  private def loadOperators(xml: Node) = {
    val operatorNodes = xml \ "CrossoverOperators" \ "CrossoverOperator"
    val operatorTypes = operatorNodes.map(_ \ "@type" text)
    operatorTypes.map(CrossoverOperator(_))
  }
}