package de.fuberlin.wiwiss.silk.workbench.learning

import de.fuberlin.wiwiss.silk.workbench.workspace.TaskData
import de.fuberlin.wiwiss.silk.learning.LearningTask

object CurrentLearningTask extends TaskData[LearningTask](new LearningTask())