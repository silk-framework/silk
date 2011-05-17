package de.fuberlin.wiwiss.silk.workbench.evaluation

import de.fuberlin.wiwiss.silk.workbench.workspace.UserData

/**
 * Keeps track of which links each user is currently viewing.
 */
object ShowLinks extends UserData[LinkType](PositiveLinks)

/**
 * The type of the evaluation links.
 */
sealed trait LinkType

/**
 * Positive reference links.
 */
case object PositiveLinks extends LinkType

/**
 * Negative reference links.
 */
case object NegativeLinks extends LinkType
