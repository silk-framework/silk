package de.fuberlin.wiwiss.silk.workbench.workspace.modules.output

import de.fuberlin.wiwiss.silk.workbench.workspace.modules.Module

/**
 * The source module which encapsulates all data sources.
 */
trait OutputModule extends Module[OutputConfig, OutputTask]
{
}