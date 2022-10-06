package models.linking

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.Dataset
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.DPair
import org.silkframework.workspace.ProjectTask

/**
  * Given an entity URI, generates a browser link that navigates to the entity.
  */
trait LinkResolver {

  /**
    * The label of the source task, where the entities are coming from.
    */
  def taskLabel: String

  /**
    * Returns a browser URL for a given entity.
    *
    * @param entityUri The URI of the entity.
    *
    */
  def apply(entityUri: String): Option[String]

}

object LinkResolver {

  /**
    * Returns a LinkResolver for a given task.
    */
  def forTask(sourceTask: Task[TaskSpec]): LinkResolver = {
    sourceTask.data match {
      case dataset: GenericDatasetSpec =>
        DatasetResolver(sourceTask.label(), dataset.plugin)
      case _ =>
        NoLinkResolver(sourceTask.label())
    }
  }

  def forLinkingTask(linkTask: ProjectTask[LinkSpec])(implicit userContext: UserContext): DPair[LinkResolver] = {
    for(selection <- linkTask.dataSelections) yield {
      forTask(linkTask.project.anyTask(selection.inputId))
    }
  }

}

/**
  * Does not generate any links for entities.
  * Used for entities from sources for which we do not have corresponding entity detail pages (CSV, etc.).
  */
case class NoLinkResolver(taskLabel: String) extends LinkResolver {

  def apply(entityUri: String): Option[String] = None

}

/**
  * Does generate links based on the dataset.
  */
case class DatasetResolver(taskLabel: String, dataset: Dataset) extends LinkResolver {

  def apply(entityUri: String): Option[String] = {
    dataset.entityLink(entityUri)
  }

}
