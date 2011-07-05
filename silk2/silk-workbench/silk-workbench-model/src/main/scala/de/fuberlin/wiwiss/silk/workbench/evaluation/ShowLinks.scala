package de.fuberlin.wiwiss.silk.workbench.evaluation

import de.fuberlin.wiwiss.silk.workbench.workspace.UserData

/**
 * Keeps track of which links each user is currently viewing.
 */
object ShowLinks extends UserData[EvalLink.ReferenceType](EvalLink.Positive)