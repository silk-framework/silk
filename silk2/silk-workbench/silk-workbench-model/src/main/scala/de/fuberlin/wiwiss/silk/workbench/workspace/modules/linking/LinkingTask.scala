package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.workbench.project.Cache
import de.fuberlin.wiwiss.silk.evaluation.Alignment
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util.Identifier

/**
 * A linking task which interlinks two datasets.
 */
case class LinkingTask(name : Identifier,
                       prefixes : Prefixes,
                       linkSpec : LinkSpecification,
                       alignment : Alignment,
                       cache : Cache) extends ModuleTask