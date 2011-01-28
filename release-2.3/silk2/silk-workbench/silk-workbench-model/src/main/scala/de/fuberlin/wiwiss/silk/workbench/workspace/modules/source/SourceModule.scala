package de.fuberlin.wiwiss.silk.workbench.workspace.modules.source

import de.fuberlin.wiwiss.silk.workbench.workspace.modules.Module

/**
 * The source module which encapsulates all data sources.
 */
trait SourceModule extends Module[SourceConfig, SourceTask]