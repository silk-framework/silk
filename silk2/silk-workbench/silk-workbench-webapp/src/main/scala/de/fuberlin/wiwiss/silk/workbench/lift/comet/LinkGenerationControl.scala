package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.TaskControl
import de.fuberlin.wiwiss.silk.workbench.workspace.User

class LinkGenerationControl extends TaskControl(User().evaluationTask, true)