package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.ProgressWidget
import de.fuberlin.wiwiss.silk.workbench.workspace.CurrentTaskStatusListener
import de.fuberlin.wiwiss.silk.workbench.learning.CurrentActiveLearningTask

class LearnProgress extends ProgressWidget(new CurrentTaskStatusListener(CurrentActiveLearningTask), hide = true) {
}