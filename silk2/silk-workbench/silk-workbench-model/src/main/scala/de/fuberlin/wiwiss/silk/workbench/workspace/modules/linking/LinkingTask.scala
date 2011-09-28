package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinks
import de.fuberlin.wiwiss.silk.workbench.workspace.Project

/**
 * A linking task which interlinks two datasets.
 */
class LinkingTask private(val linkSpec: LinkSpecification, val referenceLinks: ReferenceLinks, val cache: Cache) extends ModuleTask {
  val name = linkSpec.id

  def updateLinkSpec(linkSpec: LinkSpecification, project: Project) = {
    LinkingTask(project, linkSpec, referenceLinks, cache.update())
  }

  def updateReferenceLinks(referenceLinks: ReferenceLinks, project: Project) = {
    LinkingTask(project, linkSpec, referenceLinks, cache.update())
  }
}

object LinkingTask {
  /**
   * Constructs a new linking task and starts loading the cache.
   */
  def apply(project: Project, linkSpec: LinkSpecification, referenceLinks: ReferenceLinks = ReferenceLinks(), cache: Cache = new Cache()) = {
    val task = new LinkingTask(linkSpec, referenceLinks, cache)
    task.cache.load(project, task)
    task
  }
}