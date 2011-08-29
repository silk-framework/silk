package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.ProgressWidget
import de.fuberlin.wiwiss.silk.workbench.learning.CurrentLearningTask
import de.fuberlin.wiwiss.silk.learning.LearningTask
import de.fuberlin.wiwiss.silk.workbench.workspace.CurrentStatusListener

class LearningProgress extends ProgressWidget(new CurrentStatusListener(CurrentLearningTask)) {
}

