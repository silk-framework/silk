package de.fuberlin.wiwiss.silk.workspace.modules.transform

import de.fuberlin.wiwiss.silk.config.Dataset
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.entity.{Path, EntityDescription}
import de.fuberlin.wiwiss.silk.linkagerule.input.{TransformInput, PathInput, Input}

/**
 * A transform task, which transforms a data set.
 */
class TransformTask private(val name: Identifier, val dataset: Dataset, val rule: TransformRule, val cache: PathsCache) extends ModuleTask {

  def updateDataset(dataset: Dataset, project: Project) = {
    TransformTask(project, name, dataset, rule, cache)
  }

  def updateRule(rule: TransformRule, project: Project) = {
    TransformTask(project, name, dataset, rule, cache)
  }

  def entityDescription = {
    new EntityDescription(
      variable = dataset.variable,
      restrictions = dataset.restriction,
      paths = rule.paths.toIndexedSeq
    )
  }
}

object TransformTask {
  /**
   * Constructs a new transform task and starts loading the cache.
   */
  def apply(project: Project, name: Identifier, dataset: Dataset, rule: TransformRule, cache: PathsCache = new PathsCache()) = {
    val task = new TransformTask(name, dataset, rule, cache)
    task.cache.load(project, task)
    task
  }
}