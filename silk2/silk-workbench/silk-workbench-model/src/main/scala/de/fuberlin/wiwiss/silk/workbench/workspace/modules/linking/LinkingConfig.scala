package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import de.fuberlin.wiwiss.silk.workbench.workspace.modules.ModuleConfig
import de.fuberlin.wiwiss.silk.config.Blocking

/**
 * Configuration of a linking task.
 */
case class LinkingConfig(blocking : Option[Blocking] = Some(Blocking())) extends ModuleConfig