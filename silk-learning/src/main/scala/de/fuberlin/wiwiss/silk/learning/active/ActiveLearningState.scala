package de.fuberlin.wiwiss.silk.learning.active

import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.learning.individual.Population

case class ActiveLearningState(pool: UnlabeledLinkPool, population: Population, links: Seq[Link]) {

}

object ActiveLearningState {
  def initial = ActiveLearningState(UnlabeledLinkPool.empty, Population.empty, Seq.empty)
}
