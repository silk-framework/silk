package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.TaskControl
import de.fuberlin.wiwiss.silk.workbench.learning.CurrentLearningTask

class LearningControl extends TaskControl(CurrentLearningTask(), true)