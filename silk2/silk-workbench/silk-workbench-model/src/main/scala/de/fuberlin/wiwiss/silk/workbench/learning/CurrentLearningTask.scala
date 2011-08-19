package de.fuberlin.wiwiss.silk.workbench.learning

import de.fuberlin.wiwiss.silk.workbench.workspace.UserData
import de.fuberlin.wiwiss.silk.learning.LearningTask

object CurrentLearningTask extends UserData[LearningTask](new LearningTask())