package de.fuberlin.wiwiss.silk.workbench.learning

import de.fuberlin.wiwiss.silk.workbench.workspace.TaskData
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearningTask

object CurrentActiveLearningTask extends TaskData[ActiveLearningTask](ActiveLearningTask.empty)