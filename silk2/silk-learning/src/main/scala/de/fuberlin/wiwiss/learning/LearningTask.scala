package de.fuberlin.wiwiss.learning

import de.fuberlin.wiwiss.silk.evaluation.ReferenceInstances
import de.fuberlin.wiwiss.silk.util.task.ValueTask

/**
 * Dummy Learning Task. To be implemented...
 */
class LearningTask(instances: ReferenceInstances) extends ValueTask[Population](Population()) {
  override def execute(): Population = {
    Population()
  }
}