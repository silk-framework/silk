package org.silkframework.workspace.activity.transform

import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.entity.EntitySchema
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.util.Uri
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.{CachedActivity, PathsCacheTrait}

/**
 * Holds the most frequent paths.
 */
class TransformPathsCache(transformTask: ProjectTask[TransformSpec]) extends CachedActivity[CachedEntitySchemata] with PathsCacheTrait {

  override def name: String = s"Paths cache ${transformTask.id}"

  override def initialValue: Option[CachedEntitySchemata] = Some(CachedEntitySchemata(EntitySchema.empty, None))

  private def inputId = transformTask.data.selection.inputId

  /**
   * Loads the most frequent paths.
   */
  override def run(context: ActivityContext[CachedEntitySchemata])
                  (implicit userContext: UserContext): Unit = {
    val transform = transformTask.data

    //Create an entity description from the transformation task
    val currentEntityDesc = transform.inputSchema

    //Check if paths have not been loaded yet or if the restriction has been changed
    if (context.value().configuredSchema.typedPaths.isEmpty || currentEntityDesc.typeUri != context.value().configuredSchema.typeUri) {
      // Retrieve the data sources
      val inputTaskId = inputId
      val paths = retrievePathsOfInput(inputTaskId, Some(transform.selection), transformTask, context)
      val configuredEntitySchema = currentEntityDesc.copy(typedPaths = (currentEntityDesc.typedPaths ++ paths).distinct)
      // Retrieve untyped paths if input is an RDF data source and configured type is non empty
      val isRdfInput = transformTask.project.taskOption[DatasetSpec[Dataset]](inputTaskId).exists(_.plugin.isInstanceOf[RdfDataset])
      val unTypedEntitySchema = if (isRdfInput
          && transform.selection.typeUri.uri.nonEmpty
          && (context.value().untypedSchema.isEmpty
          || context.value().untypedSchema.get.typedPaths.isEmpty)) {
        val selection = transform.selection.copy(typeUri = Uri(""))
        val unTypedPaths = retrievePathsOfInput(inputTaskId, Some(selection), transformTask, context)
        Some(currentEntityDesc.copy(typeUri = Uri(""), typedPaths = (currentEntityDesc.typedPaths ++ unTypedPaths).distinct))
      } else {
        None
      }
      //Add the frequent paths to the entity description
      context.value() = CachedEntitySchemata(configuredEntitySchema, unTypedEntitySchema)
    }
  }

  override def resource: WritableResource = transformTask.project.cacheResources.child("transform").child(transformTask.id).get(s"pathsCache.xml")

  override protected val wrappedXmlFormat = WrappedXmlFormat()
}