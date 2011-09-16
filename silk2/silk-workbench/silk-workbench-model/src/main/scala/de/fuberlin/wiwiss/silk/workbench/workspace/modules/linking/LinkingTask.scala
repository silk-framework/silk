package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinks
import de.fuberlin.wiwiss.silk.workbench.workspace.Project
import de.fuberlin.wiwiss.silk.util.task.TaskStatus

/**
 * A linking task which interlinks two datasets.
 */
class LinkingTask private(val linkSpec: LinkSpecification, val referenceLinks: ReferenceLinks, val cache: Cache) extends ModuleTask {
  val name = linkSpec.id

  def updateLinkSpec(linkSpec: LinkSpecification, project: Project) = {
    val task: LinkingTask = new LinkingTask(linkSpec, referenceLinks, cache.update())
    task.cache.load(project, task)
    task
  }

  def updateReferenceLinks(referenceLinks: ReferenceLinks, project: Project) = {
    val task: LinkingTask = new LinkingTask(linkSpec, referenceLinks, cache.update())
    task.cache.load(project, task)
    task
  }
}

object LinkingTask {
  def apply(linkSpec: LinkSpecification, referenceLinks: ReferenceLinks = ReferenceLinks(), cache: Cache = new Cache()) = {
    new LinkingTask(linkSpec, referenceLinks, cache)
  }
}