package de.fuberlin.wiwiss.silk.workbench.learning

import de.fuberlin.wiwiss.silk.workbench.workspace.UserData
import de.fuberlin.wiwiss.silk.entity.Link

object CurrentPool extends UserData[Traversable[Link]](Traversable[Link]())