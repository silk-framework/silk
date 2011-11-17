package de.fuberlin.wiwiss.silk.workbench.learning

import de.fuberlin.wiwiss.silk.workbench.workspace.TaskData
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration

object CurrentConfiguration extends TaskData[LearningConfiguration](LearningConfiguration.load())