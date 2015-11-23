package org.silkframework.learning.active

import org.silkframework.entity.Link
import org.silkframework.learning.individual.Population

case class ActiveLearningState(pool: UnlabeledLinkPool, population: Population, links: Seq[Link]) {

}

object ActiveLearningState {
  def initial = ActiveLearningState(UnlabeledLinkPool.empty, Population.empty, Seq.empty)
}
