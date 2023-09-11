package org.silkframework.workspace.activity.linking

import org.silkframework.config.{DefaultConfig, Prefixes}
import org.silkframework.entity.EntitySchema
import org.silkframework.rule.{DatasetSelection, LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.util.DPair
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.{CachedActivity, PathsCacheTrait}

import scala.collection.mutable

/**
 * Holds the most frequent paths.
 */
class LinkingPathsCache(task: ProjectTask[LinkSpec]) extends CachedActivity[DPair[EntitySchema]] with PathsCacheTrait {

  final val MAX_PATHS_DEFAULT = 50

  // FIXME: Make configurable or set to Int.MaxValue
  override def maxDepth: Int = 3

  override protected val maxPaths: Option[Int] = Some{
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
  private var transformSpecObserverFunctions: Option[TransformSpec => Unit] = None

  private def setTransformSpecObserverFunction()(implicit userContext: UserContext): Unit = {
    val fn: TransformSpec => Unit = _ => {
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
  override def loadCache(context: ActivityContext[DPair[EntitySchema]], fullReload: Boolean)
                        (implicit userContext: UserContext): Unit = {
    if(transformSpecObserverFunctions.isEmpty) {
      setTransformSpecObserverFunction()
    }
    context.status.update("Retrieving frequent property paths", 0.0)

    val currentEntityDescs = linkSpec.entityDescriptions
    val (typeChanged: Boolean, emptyPaths: Boolean, update: Boolean) = updateCache(context, currentEntityDescs, fullReload)

    // Update paths
    if (update) {
      val updatedSchemata =
        for((dataSelection, entitySchema) <- linkSpec.dataSelections zip currentEntityDescs) yield {
          if(fullReload && !(emptyPaths || typeChanged)) {
            // Only transformation sources changed
            if(task.project.taskOption[TransformSpec](dataSelection.inputId).isDefined) {
              updateSchema(dataSelection, entitySchema, context)
            } else {
              entitySchema // Do not update other source schemata automatically
            }
          } else {
            updateSchema(dataSelection, entitySchema, context)
          }
        }
      context.value.update(updatedSchemata)
    }
  }

  private def updateCache(context: ActivityContext[DPair[EntitySchema]], currentEDs: DPair[EntitySchema], fullReload: Boolean) = {
    //Create an entity description from the link specification

    //Check if the restriction has been changed
    val typeChanged = currentEDs.source.typeUri != context.value().source.typeUri ||
        currentEDs.target.typeUri != context.value().target.typeUri
    val restrictionChanged = currentEDs.source.filter != context.value().source.filter ||
        currentEDs.target.filter != context.value().target.filter
    val emptyPaths = context.value().source.typedPaths.isEmpty && context.value().target.typedPaths.isEmpty
    val schemaNotFullyCovered = !currentLinkageRuleSchemaIsCovered(currentEDs.source, context.value().source) ||
      !currentLinkageRuleSchemaIsCovered(currentEDs.target, context.value().target)
    val update =
      emptyPaths ||
        restrictionChanged ||
        typeChanged ||
        fullReload ||
        schemaNotFullyCovered
    (typeChanged, emptyPaths, update)
  }

  private def currentLinkageRuleSchemaIsCovered(linkingSchema: EntitySchema, currentCachedSchema: EntitySchema): Boolean = {
    val notCovered = mutable.HashSet[String](linkingSchema.typedPaths.map(_.normalizedSerialization) :_*)
    val it = currentCachedSchema.typedPaths.iterator
    while(notCovered.nonEmpty && it.hasNext) {
      val next = it.next()
      if(notCovered.contains(next.normalizedSerialization)) {
        notCovered.remove(next.normalizedSerialization)
      }
    }
    notCovered.isEmpty
  }

  private def updateSchema(datasetSelection: DatasetSelection,
                           entitySchema: EntitySchema,
                           context: ActivityContext[DPair[EntitySchema]])
                          (implicit userContext: UserContext): EntitySchema = {
    implicit val prefixes: Prefixes = task.project.config.prefixes
    val paths = retrievePathsOfInput(datasetSelection.inputId, Some(datasetSelection), task.project, context)
    entitySchema.adapt(typedPaths = (paths ++ entitySchema.typedPaths).distinct)
  }

  override def resource: WritableResource = task.project.cacheResources.child("linking").child(task.id).get(s"pathsCache.xml")

  override protected val wrappedXmlFormat = WrappedXmlFormat()
}
