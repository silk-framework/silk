package de.fuberlin.wiwiss.silk.workbench.evaluation

import de.fuberlin.wiwiss.silk.workbench.workspace.TaskData

/**
 * Keeps track of which links each user is currently viewing.
 */
object ShowLinks extends TaskData[EvalLink.ReferenceType](EvalLink.Positive)