package de.fuberlin.wiwiss.silk.workspace

import de.fuberlin.wiwiss.silk.config.{RuntimeConfig, LinkSpecification, TransformSpecification}
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.entity.rdf.SparqlEntitySchema
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.execution.{GenerateLinks, ExecuteTransform}
import de.fuberlin.wiwiss.silk.learning.active.{ActiveLearning, ActiveLearningState}
import de.fuberlin.wiwiss.silk.learning.{LearningConfiguration, LearningActivity, LearningInput, LearningResult}
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.{Types, TypesCache}
import de.fuberlin.wiwiss.silk.workspace.modules.linking.{LinkingPathsCache, ReferenceEntitiesCache}
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformPathsCache
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.{WorkflowExecutor, Workflow}
import de.fuberlin.wiwiss.silk.workspace.modules.{TaskActivity, Task}

import scala.reflect.ClassTag

case class DefaultActivities() extends ActivityProvider {

  override def taskActivities[T: ClassTag](project: Project, task: Task[T]): Seq[TaskActivity[_, _]] = {
    task match {
      case t if t.data.isInstanceOf[Dataset] => datasetActivities(project, t.asInstanceOf[Task[Dataset]])
      case t if t.data.isInstanceOf[TransformSpecification] => transformActivities(project, t.asInstanceOf[Task[TransformSpecification]])
      case t if t.data.isInstanceOf[LinkSpecification] => linkingActivities(project, t.asInstanceOf[Task[LinkSpecification]])
      case t if t.data.isInstanceOf[Workflow] => workflowActivities(project, t.asInstanceOf[Task[Workflow]])
      case _ => Seq.empty
    }
  }

  private def datasetActivities(project: Project, task: Task[Dataset]): Seq[TaskActivity[_,_]] = {
    // Types cache
    def typesCache() = new TypesCache(task.data)
    // Create task activities
    TaskActivity(s"${task.name}_cache.xml", Types.empty, typesCache, project.cacheResources.child("dataset")) :: Nil
  }

  private def transformActivities(project: Project, task: Task[TransformSpecification]): Seq[TaskActivity[_,_]] = {
    // Execute transform
    def executeTransform =
      new ExecuteTransform(
        input = project.task[Dataset](task.data.selection.datasetId).data.source,
        selection = task.data.selection,
        rules = task.data.rules,
        outputs = task.data.outputs.map(id => project.task[Dataset](id).data.sink)
      )
    def pathsCache() =
      new TransformPathsCache(
        dataset = project.task[Dataset](task.data.selection.datasetId).data,
        transform = task.data
      )
    // Create task activities
    TaskActivity(executeTransform) ::
    TaskActivity("cache.xml", null: SparqlEntitySchema, pathsCache, project.cacheResources.child("transform").child(task.name))  :: Nil
  }

  private def linkingActivities(project: Project, task: Task[LinkSpecification]) = {
    // Generate links
    def generateLinks(links: Seq[Link]) =
      GenerateLinks.fromSources(
        datasets = project.tasks[Dataset].map(_.data),
        linkSpec = task.data,
        runtimeConfig = RuntimeConfig(useFileCache = false, partitionSize = 300, generateLinksWithEntities = true)
      )

    // Supervised learning
    def learning(population: LearningResult) = {
      val input =
        LearningInput(
          trainingEntities = task.activity[ReferenceEntitiesCache].value(),
          seedLinkageRules = task.data.rule :: Nil
        )
      new LearningActivity(input, LearningConfiguration.default)
    }

    // Active learning
    def activeLearning(state: ActiveLearningState) =
      new ActiveLearning(
        config = LearningConfiguration.default,
        datasets = DPair.fromSeq(task.data.dataSelections.map(ds => project.tasks[Dataset].map(_.data).find(_.id == ds.datasetId).getOrElse(Dataset.empty).source)),
        linkSpec = task.data,
        paths = task.activity[LinkingPathsCache].value().map(_.paths),
        referenceEntities = task.activity[ReferenceEntitiesCache].value(),
        state = state
      )

    // Paths Cache
    def pathsCache() =
      new LinkingPathsCache(
        datasets = task.data.dataSelections.map(ds => project.task[Dataset](ds.datasetId).data),
        linkSpec = task.data
      )

    // ReferenceEntities Cache
    def referenceEntitiesCache() = new ReferenceEntitiesCache(task, project)

    // Create task activities
    val taskResources = project.cacheResources.child("linking").child(task.name)

    TaskActivity(Seq[Link](), generateLinks) ::
    TaskActivity("pathsCache.xml", null: DPair[SparqlEntitySchema], pathsCache, taskResources) ::
    TaskActivity("referenceEntitiesCache.xml", ReferenceEntities.empty, referenceEntitiesCache, taskResources) ::
    TaskActivity(LearningResult(), learning) ::
    TaskActivity(ActiveLearningState.initial, activeLearning) :: Nil
  }

  private def workflowActivities(project: Project, task: Task[Workflow]): Seq[TaskActivity[_,_]] = {
    def workflowExecutor = new WorkflowExecutor(task.data.operators, project)
    TaskActivity(workflowExecutor) :: Nil
  }

}