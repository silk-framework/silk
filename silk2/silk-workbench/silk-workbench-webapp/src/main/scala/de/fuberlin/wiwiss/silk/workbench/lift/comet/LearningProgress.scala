package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.ProgressWidget
import de.fuberlin.wiwiss.silk.workbench.learning.CurrentLearningTask
import de.fuberlin.wiwiss.silk.workbench.workspace.User._
import collection.mutable.{Publisher, Subscriber}
import de.fuberlin.wiwiss.silk.workbench.workspace.UserData.ValueUpdated
import de.fuberlin.wiwiss.silk.learning.LearningTask

class LearningProgress extends ProgressWidget(CurrentLearningTask()) {

  /** Listen to changes of the current learning task. */
  CurrentLearningTask.subscribe(new Subscriber[ValueUpdated[LearningTask], Publisher[ValueUpdated[LearningTask]]] {
    def notify(pub : Publisher[ValueUpdated[LearningTask]], event : ValueUpdated[LearningTask]) {
      reRender()
    }
  })
}

