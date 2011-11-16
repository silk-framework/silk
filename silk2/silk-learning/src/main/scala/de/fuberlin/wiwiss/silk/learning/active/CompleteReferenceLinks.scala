package de.fuberlin.wiwiss.silk.learning.active

import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceEntities, ReferenceLinks}
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule

object CompleteReferenceLinks {
  def apply(referenceEntities: ReferenceEntities, unlabeledLinks: Traversable[Link], population: Population) = {
    implicit lazy val rules = population.individuals.map(_.node.build)

    val positive = {
      if(!referenceEntities.positive.isEmpty) {
        referenceEntities.positive
      } else {
        val maxLink = unlabeledLinks.map(withAgreement).maxBy(_.confidence)
        Map(maxLink -> maxLink.entities.get)
      }
    }

    val negative = {
      if(!referenceEntities.negative.isEmpty) {
        referenceEntities.negative
      } else {
        val minLink = unlabeledLinks.map(withAgreement).minBy(_.confidence)
        Map(minLink -> minLink.entities.get)
      }
    }

    ReferenceEntities(positive, negative)
  }

  def withAgreement(link: Link)(implicit rules: Traversable[LinkageRule]) = {
    val confidenceSum = rules.map(_.apply(link.entities.get)).sum
    val confidence = confidenceSum / rules.size

    link.update(confidence = Some(confidence))
  }
}