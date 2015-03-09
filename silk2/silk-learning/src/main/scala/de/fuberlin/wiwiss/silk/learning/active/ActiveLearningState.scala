package de.fuberlin.wiwiss.silk.learning.active

import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.learning.individual.Population

case class ActiveLearningState(pool: Traversable[Link], population: Population, links: Seq[Link]) {

}

object ActiveLearningState {
  def initial = ActiveLearningState(Traversable.empty, Population.empty, Seq.empty)
}
