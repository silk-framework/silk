package de.fuberlin.wiwiss.silk.workspace

import de.fuberlin.wiwiss.silk.config.{LinkSpecification, RuntimeConfig, TransformSpecification}
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.entity.rdf.SparqlEntitySchema
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.execution.{ExecuteTransform, GenerateLinks}
import de.fuberlin.wiwiss.silk.learning.active.{ActiveLearning, ActiveLearningState}
import de.fuberlin.wiwiss.silk.learning.{LearningActivity, LearningConfiguration, LearningInput, LearningResult}
import de.fuberlin.wiwiss.silk.runtime.activity.{HasValue, ActivityContext, Activity}
import de.fuberlin.wiwiss.silk.runtime.plugin.AnyPlugin
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workspace.modules.Task
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.{Types, TypesCache}
import de.fuberlin.wiwiss.silk.workspace.modules.linking.{LinkingPathsCache, ReferenceEntitiesCache}
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformPathsCache
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.{Workflow, WorkflowExecutor}
import de.fuberlin.wiwiss.silk.util.StringUtils._

import scala.reflect.ClassTag

/**
  * Factory for generating activities that belong to a task.
  *
  * @tparam TaskType The type of the task the generate activities belong to
  * @tparam ActivityType The type of activity that is generated and by which the activity will be identified within the task
  * @tparam ActivityData The type of the activity values.
  */
abstract class TaskActivityFactory[TaskType: ClassTag, ActivityType <: Activity[ActivityData] : ClassTag, ActivityData] extends AnyPlugin {

  /** True, if this activity shall be executed automatically after startup */
  def autoRun: Boolean = false

  /**
    * Generates a new activity for a given task.
    */
  def apply(task: Task[TaskType]): Activity[ActivityData]

  /**
    * Checks, if this factory generates activities for a given task type
    */
  def isTaskType[T: ClassTag]: Boolean = {
    implicitly[ClassTag[TaskType]].runtimeClass == implicitly[ClassTag[T]].runtimeClass
  }

  /**
    * Returns the type of generated activities.
    */
  def activityType = implicitly[ClassTag[ActivityType]].runtimeClass
}

class TypesCacheFactory extends TaskActivityFactory[Dataset, TypesCache, Types] {

  override def autoRun = true

  def apply(task: Task[Dataset]): Activity[Types] = {
    new CachedActivity(
      activity = new TypesCache(task.data),
      resource = task.project.cacheResources.child("dataset").get(s"${task.name}_cache.xml")
    )
  }
}

class ExecuteTransformFactory extends TaskActivityFactory[TransformSpecification, ExecuteTransform, Unit] {

  def apply(task: Task[TransformSpecification]): Activity[Unit] = {
    Activity.regenerating {
      new ExecuteTransform(
        input = task.project.task[Dataset](task.data.selection.datasetId).data.source,
        selection = task.data.selection,
        rules = task.data.rules,
        outputs = task.data.outputs.map(id => task.project.task[Dataset](id).data.sink)
      )
    }
  }
}

class TransformPathsCacheFactory extends TaskActivityFactory[TransformSpecification, TransformPathsCache, SparqlEntitySchema] {

  def apply(task: Task[TransformSpecification]) = {
    new CachedActivity(
      activity = new TransformPathsCache(task),
      resource = task.project.cacheResources.child("transform").child(task.name).get(s"pathsCache.xml")
    )
  }
}

class GenerateLinksFactory extends TaskActivityFactory[LinkSpecification, GenerateLinks, Seq[Link]] {

  def apply(task: Task[LinkSpecification]): Activity[Seq[Link]] = {
    Activity.regenerating {
      GenerateLinks.fromSources(
        datasets = task.project.tasks[Dataset].map(_.data),
        linkSpec = task.data,
        runtimeConfig = RuntimeConfig(useFileCache = false, partitionSize = 300, generateLinksWithEntities = true)
      )
    }
  }
}

class LearningFactory extends TaskActivityFactory[LinkSpecification, LearningActivity, LearningResult] {

  def apply(task: Task[LinkSpecification]): Activity[LearningResult] = {
    Activity.regenerating {
      val input =
        LearningInput(
          trainingEntities = task.activity[ReferenceEntitiesCache].value(),
          seedLinkageRules = task.data.rule :: Nil
        )
      new LearningActivity(input, LearningConfiguration.default)
    }
  }

}

class ActiveLearningFactory extends TaskActivityFactory[LinkSpecification, ActiveLearning, ActiveLearningState] {

  def apply(task: Task[LinkSpecification]): Activity[ActiveLearningState] = {
    Activity.regenerating {
      new ActiveLearning(
        config = LearningConfiguration.default,
        datasets = DPair.fromSeq(task.data.dataSelections.map(ds => task.project.tasks[Dataset].map(_.data).find(_.id == ds.datasetId).getOrElse(Dataset.empty).source)),
        linkSpec = task.data,
        paths = task.activity[LinkingPathsCache].value().map(_.paths),
        referenceEntities = task.activity[ReferenceEntitiesCache].value()
      )
    }
  }

}

class LinkingPathsCacheFactory extends TaskActivityFactory[LinkSpecification, LinkingPathsCache, DPair[SparqlEntitySchema]] {

  override def autoRun = true

  def apply(task: Task[LinkSpecification]): Activity[DPair[SparqlEntitySchema]] = {
    new CachedActivity(
      activity =
        new LinkingPathsCache(
          datasets = task.data.dataSelections.map(ds => task.project.task[Dataset](ds.datasetId).data),
          linkSpec = task.data
        ),
      resource = task.project.cacheResources.child("linking").child(task.name).get(s"pathsCache.xml")
    )
  }
}

class ReferenceEntitiesCacheFactory extends TaskActivityFactory[LinkSpecification, ReferenceEntitiesCache, ReferenceEntities] {

  override def autoRun = true

  def apply(task: Task[LinkSpecification]): Activity[ReferenceEntities] = {
    new CachedActivity(
      activity = new ReferenceEntitiesCache(task),
      resource = task.project.cacheResources.child("linking").child(task.name).get(s"referenceEntitiesCache.xml")
    )
  }
}

class WorkflowExecutorFactory extends TaskActivityFactory[Workflow, WorkflowExecutor, Unit] {

  override def apply(task: Task[Workflow]): Activity[Unit] = {
    new WorkflowExecutor(task)
  }

}