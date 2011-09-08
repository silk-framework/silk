package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import de.fuberlin.wiwiss.silk.workbench.workspace.modules.ModuleConfig
import de.fuberlin.wiwiss.silk.config.RuntimeConfig

/**
 * SilkConfig of a linking task.
 */
case class LinkingConfig(runtime: RuntimeConfig = RuntimeConfig()) extends ModuleConfig