package org.silkframework.workspace.activity.linking

import org.silkframework.config.DefaultConfig
import org.silkframework.dataset.{DataSource, DatasetSpec, SparqlRestrictionDataSource}
import org.silkframework.entity.Restriction.CustomOperator
import org.silkframework.entity.rdf.SparqlRestriction
import org.silkframework.entity.{EntitySchema, Path}
import org.silkframework.rule.{DatasetSelection, LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.util.DPair
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.linking.LinkingTaskUtils._
import org.silkframework.workspace.activity.{CachedActivity, PathsCacheTrait}

/**
 * Holds the most frequent paths.
 */
class LinkingPathsCache(task: ProjectTask[LinkSpec]) extends CachedActivity[DPair[EntitySchema]] with PathsCacheTrait {

  final val MAX_PATHS_DEFAULT = 50

  private val maxLinks = {
    val cfg = DefaultConfig.instance()
    val key = "caches.linkingPathCache.maxLinks"
    if(cfg.hasPath(key)) {
      cfg.getInt(key)
    } else { MAX_PATHS_DEFAULT }
  }

  private def linkSpec = task.data

  override def name: String = s"Paths cache ${task.id}"

  override def initialValue: Option[DPair[EntitySchema]] = Some(DPair.fill(EntitySchema.empty))

  /** The purpose of this value is to store the change notify callback function
    * because it will be in a WeakHashMap in the Observable and would else be garbage collected */
  private var transformSpecObserverFunctions: Option[(TransformSpec) => Unit] = None

  private def setTransformSpecObserverFunction()(implicit userContext: UserContext) {
    val fn: (TransformSpec) => Unit = (_) => {
      this.startDirty(task.activity[LinkingPathsCache].control)
    }
    for(selection <- task.data.dataSelections) {
      val sourceInputId = selection.inputId
      // Only do this automatically if the input is a transformation
      task.project.taskOption[TransformSpec](sourceInputId).foreach { inputTask =>
        inputTask.dataValueHolder.subscribe(fn)
      }
    }
    transformSpecObserverFunctions = Some(fn)
  }

  /**
   * Loads the most frequent property paths.
   */
  override def run(context: ActivityContext[DPair[EntitySchema]])
                  (implicit userContext: UserContext): Unit = {
    if(transformSpecObserverFunctions.isEmpty) {
      setTransformSpecObserverFunction()
    }
    context.status.update("Retrieving frequent property paths", 0.0)

    //Create an entity description from the link specification
    val currentEntityDescs = linkSpec.entityDescriptions

    //Check if the restriction has been changed
    val typeChanged = currentEntityDescs.source.typeUri != context.value().source.typeUri &&
        currentEntityDescs.target.typeUri != context.value().target.typeUri
    val emptyPaths = context.value().source.typedPaths.isEmpty && context.value().target.typedPaths.isEmpty
    val update = emptyPaths ||
        typeChanged ||
        dirty

    // Update paths
    if (update) {
      val updatedSchemata =
        for((dataSelection, entitySchema) <- linkSpec.dataSelections zip currentEntityDescs) yield {
          if(dirty && !(emptyPaths || typeChanged)) {
            dirty = false
            // Only transformation sources changed
            if(task.project.taskOption[TransformSpec](dataSelection.inputId).isDefined) {
              updateSchema(dataSelection, entitySchema)
            } else {
              entitySchema // Do not update other source schemata automatically
            }
          } else {
            updateSchema(dataSelection, entitySchema)
          }
        }
      context.value.update(updatedSchemata)
    }
  }

  private def updateSchema(datasetSelection: DatasetSelection,
                           entitySchema: EntitySchema)
                          (implicit userContext: UserContext): EntitySchema = {
    val paths = retrievePaths(datasetSelection)
    entitySchema.copy(typedPaths = (entitySchema.typedPaths ++ paths.map(_.asStringTypedPath)).distinct)
  }

  private def retrievePaths(datasetSelection: DatasetSelection)
                           (implicit userContext: UserContext): IndexedSeq[Path] = {
    // Retrieve the data source
    task.dataSource(datasetSelection) match {
      case DatasetSpec.DataSourceWrapper(ds: SparqlRestrictionDataSource, _) =>
        val typeRestriction = SparqlRestriction.forType(datasetSelection.typeUri)
        val sparqlRestriction = datasetSelection.restriction.operator match {
          case Some(CustomOperator(sparqlExpression)) =>
            SparqlRestriction.fromSparql("a", sparqlExpression).merge(typeRestriction)
          case _ =>
            typeRestriction
        }
        ds.retrievePathsSparqlRestriction(sparqlRestriction, Some(maxLinks))
      case source: DataSource =>
        // Retrieve most frequent paths
        source.retrievePaths(datasetSelection.typeUri, 1, Some(maxLinks))
    }
  }

  override def resource: WritableResource = task.project.cacheResources.child("linking").child(task.id).get(s"pathsCache.xml")

  override protected val wrappedXmlFormat = WrappedXmlFormat()
}
