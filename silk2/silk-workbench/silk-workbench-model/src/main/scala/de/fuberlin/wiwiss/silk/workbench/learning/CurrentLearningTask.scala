package de.fuberlin.wiwiss.silk.learning

import de.fuberlin.wiwiss.silk.workbench.workspace.UserData
import de.fuberlin.wiwiss.silk.workbench.workspace.User

object CurrentLearningTask extends UserData[LearningTask](new LearningTask(User().linkingTask.cache.instances))