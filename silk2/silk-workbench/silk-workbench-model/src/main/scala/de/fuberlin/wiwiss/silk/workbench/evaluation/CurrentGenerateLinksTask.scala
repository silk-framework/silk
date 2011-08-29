package de.fuberlin.wiwiss.silk.workbench.evaluation

import de.fuberlin.wiwiss.silk.workbench.workspace.{User, UserData}

object CurrentGenerateLinksTask extends UserData[GenerateLinksTask](new GenerateLinksTask(User()))