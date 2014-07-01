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
  require(rules.map(_.name).distinct.size == rules.size, "Rule names must be unique.")

  def updateDataset(dataset: Dataset, project: Project) = {
    TransformTask(project, name, dataset, rules, cache)
  }

  def updateRule(rule: TransformRule, project: Project) = {
    val updatedRules = rules.updated(rules.indexWhere(_.name == rule.name), rule)
    TransformTask(project, name, dataset, updatedRules, cache)
  }

  def updateRules(rules: Seq[TransformRule], project: Project) = {
    TransformTask(project, name, dataset, rules, cache)
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