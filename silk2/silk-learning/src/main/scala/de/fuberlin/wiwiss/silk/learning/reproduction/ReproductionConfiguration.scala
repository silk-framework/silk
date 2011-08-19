package de.fuberlin.wiwiss.silk.learning.reproduction

import xml.Node

case class ReproductionConfiguration() {

  val mutationProbability = 0.25

  val elitismCount = 3

  val tournamentSize = 5
}

object ReproductionConfiguration
{
  def fromXml(xml: Node) = {
    ReproductionConfiguration(
    )
  }

  private def loadOperators(xml: Node) = {
    val operatorNodes = xml \ "CrossoverOperators" \ "CrossoverOperator"
    val operatorTypes = operatorNodes.map(_ \ "@type" text)
    operatorTypes.map(CrossoverOperator(_))
  }
}