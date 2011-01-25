package de.fuberlin.wiwiss.silk.workbench.workspace.modules.source

import de.fuberlin.wiwiss.silk.workbench.workspace.modules.ModuleTask

/**
 * A data source.
 */
//TODO replace endpointUri with complete datasource definition
case class SourceTask(name : String, endpointUri : String) extends ModuleTask