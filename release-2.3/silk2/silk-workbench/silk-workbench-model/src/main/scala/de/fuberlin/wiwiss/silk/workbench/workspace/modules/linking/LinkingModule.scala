package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import de.fuberlin.wiwiss.silk.workbench.workspace.modules.Module

/**
 * The linking module which encapsulates all linking tasks.
 */
trait LinkingModule extends Module[LinkingConfig, LinkingTask]