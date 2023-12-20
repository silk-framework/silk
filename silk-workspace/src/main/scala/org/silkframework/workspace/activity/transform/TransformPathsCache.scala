package org.silkframework.workspace.activity.transform

import org.silkframework.config.{CustomTask, Prefixes, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.entity.EntitySchema
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.util.{Identifier, Uri}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.{CachedActivity, PathsCacheTrait}

/**
 * Holds the most frequent paths.
 */
class TransformPathsCache(transformTask: ProjectTask[TransformSpec]) extends CachedActivity[CachedEntitySchemata] with PathsCacheTrait {

  override def name: String = s"Paths cache ${transformTask.id}"

  override def initialValue: Option[CachedEntitySchemata] = Some(CachedEntitySchemata(EntitySchema.empty, None, inputId, None))

  protected override def maxDepth: Int = Int.MaxValue

  private def inputId = transformTask.data.selection.inputId

  /** The purpose of this value is to store the change notify callback function
    * because it will be in a WeakHashMap in the Observable and would else be garbage collected */
  private var datasetObserverFunctions: Option[TaskSpec => Unit] = None
  private var observedInputTask: Option[Identifier] = None

  private def setTransformSpecObserverFunction()(implicit userContext: UserContext): Unit = {
    val fn: TaskSpec => Unit = _ => {
      this.startDirty(transformTask.activity[TransformPathsCache].control)
    }
    // Only do this automatically if the input is a dataset
    val inputId = transformTask.data.selection.inputId
    val inputDatasetOpt = transformTask.project.taskOption[GenericDatasetSpec](inputId)
    inputDatasetOpt.foreach { inputDataset =>
      inputDataset.dataValueHolder.subscribe(fn)
    }
    // or a CustomTask
    val inputCustomTaskOpt = transformTask.project.taskOption[CustomTask](inputId)
    inputCustomTaskOpt.foreach { inputTask =>
      inputTask.dataValueHolder.subscribe(fn)
    }
    datasetObserverFunctions = Some(fn)
    observedInputTask = Some(inputId)
  }

  /**
   * Loads the most frequent paths.
   */
  override def loadCache(context: ActivityContext[CachedEntitySchemata], fullReload: Boolean)
                        (implicit userContext: UserContext): Unit = {
    val transform = transformTask.data
    implicit val prefixes: Prefixes = transformTask.project.config.prefixes
    implicit val pluginContext: PluginContext = PluginContext.fromProject(transformTask.project)

    if(datasetObserverFunctions.isEmpty || !observedInputTask.contains(transformTask.data.selection.inputId)) {
      setTransformSpecObserverFunction()
    }

    //Create an entity description from the transformation task
    val currentEntityDesc = transform.inputSchema
    // The parameters of the input dataset, if any
    val datasetParams = datasetParameters()

    //Check if paths have not been loaded yet or if the restriction has been changed
    if (context.value().configuredSchema.typedPaths.isEmpty ||
        currentEntityDesc.typeUri != context.value().configuredSchema.typeUri ||
        inputId != context.value().inputTaskId ||
        datasetParams != context.value().datasetParameters ||
        fullReload) {
      val currentCachedValue = context.value()
      // Set current cache value to empty
      context.value() = currentCachedValue.copy(configuredSchema = currentEntityDesc.copy(typedPaths = IndexedSeq.empty), untypedSchema = None)
      // Retrieve the data sources
      val inputTaskId = inputId
      val paths = retrievePathsOfInput(inputTaskId, Some(transform.selection), transformTask.project, context)
      val configuredEntitySchema = currentEntityDesc.copy(typedPaths = paths.distinct)
      // Retrieve untyped paths if input is an RDF data source and configured type is non empty
      val isRdfInput = transformTask.project.taskOption[DatasetSpec[Dataset]](inputTaskId).exists(_.plugin.isInstanceOf[RdfDataset])
      val unTypedEntitySchema = if (isRdfInput
          && transform.selection.typeUri.uri.nonEmpty
          && (context.value().untypedSchema.isEmpty
          || context.value().untypedSchema.get.typedPaths.isEmpty)) {
        val selection = transform.selection.copy(typeUri = Uri(""))
        val unTypedPaths = retrievePathsOfInput(inputTaskId, Some(selection), transformTask.project, context)
        Some(currentEntityDesc.copy(typeUri = Uri(""), typedPaths = unTypedPaths.distinct))
      } else {
        None
      }
      //Add the frequent paths to the entity description
      context.value() = CachedEntitySchemata(configuredEntitySchema, unTypedEntitySchema, inputTaskId, datasetParams)
    }
  }

  override def resource: WritableResource = transformTask.project.cacheResources.child("transform").child(transformTask.id).get(s"pathsCache.xml")

  override protected val wrappedXmlFormat = WrappedXmlFormat()

  private def datasetParameters()(implicit userContext: UserContext,
                                  pluginContext: PluginContext) = {
    transformTask.project.anyTask(inputId).data match {
      case dataset: DatasetSpec[Dataset] =>
        Some(dataset.plugin.parameters)
      case _ =>
        None
    }
  }
}