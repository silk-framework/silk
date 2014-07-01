package de.fuberlin.wiwiss.silk.workspace.modules.transform

import de.fuberlin.wiwiss.silk.config.Dataset
import de.fuberlin.wiwiss.silk.entity.EntityDescription
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask

/**
 * A transform task, which transforms a data set.
 */
class TransformTask private(val name: Identifier, val dataset: Dataset, val rules: Seq[TransformRule], val cache: PathsCache) extends ModuleTask {

  def updateDataset(dataset: Dataset, project: Project) = {
    TransformTask(project, name, dataset, rules, cache)
  }

  def updateRule(rule: TransformRule, ruleIndex: Int, project: Project) = {
    TransformTask(project, name, dataset, rules.updated(ruleIndex, rule), cache)
  }

  def entityDescription = {
    new EntityDescription(
      variable = dataset.variable,
      restrictions = dataset.restriction,
      paths = rules.flatMap(_.paths).distinct.toIndexedSeq
    )
  }
}

object TransformTask {
  /**
   * Constructs a new transform task and starts loading the cache.
   */
  def apply(project: Project, name: Identifier, dataset: Dataset, rules: Seq[TransformRule], cache: PathsCache = new PathsCache()) = {
    val task = new TransformTask(name, dataset, rules, cache)
    task.cache.load(project, task)
    task
  }
}