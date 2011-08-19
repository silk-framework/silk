package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.ProgressWidget
import de.fuberlin.wiwiss.silk.workbench.learning.CurrentLearningTask
import de.fuberlin.wiwiss.silk.learning.LearningTask

class LearningProgress extends ProgressWidget(CurrentLearningTask()) {

  /** Listen to changes of the current learning task. */
  CurrentLearningTask.onUpdate(CurrentLearningTaskListener)

  private object CurrentLearningTaskListener extends (LearningTask => Unit) {
    def apply(task: LearningTask) {
      reRender()
    }
  }
}

