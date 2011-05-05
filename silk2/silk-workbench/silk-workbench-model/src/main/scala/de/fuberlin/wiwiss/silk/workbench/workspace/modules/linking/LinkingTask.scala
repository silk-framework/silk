package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.evaluation.Alignment

/**
 * A linking task which interlinks two datasets.
 */
case class LinkingTask(linkSpec : LinkSpecification,
                       alignment : Alignment = Alignment(),
                       cache : Cache = new Cache()) extends ModuleTask
{
  val name = linkSpec.id
}