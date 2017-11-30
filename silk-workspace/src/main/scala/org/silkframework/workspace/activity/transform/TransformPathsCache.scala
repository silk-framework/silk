package org.silkframework.workspace.activity.transform

import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.entity.{EntitySchema, PathOperator, TypedPath}
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.util.Uri
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.PathsCacheTrait

import scala.xml.{Node, Null}

/**
 * Holds the most frequent paths.
 */
class TransformPathsCache(transformTask: ProjectTask[TransformSpec]) extends Activity[CachedEntitySchemata] with PathsCacheTrait {

  override def name: String = s"Paths cache ${transformTask.id}"

  override def initialValue: Option[CachedEntitySchemata] = Some(CachedEntitySchemata(EntitySchema.empty, None))

  /**
   * Loads the most frequent paths.
   */
  override def run(context: ActivityContext[CachedEntitySchemata]): Unit = {
    val transform = transformTask.data

    //Create an entity description from the transformation task
    val currentEntityDesc = transform.inputSchema

    //Check if paths have not been loaded yet or if the restriction has been changed
    if (context.value().configuredSchema.typedPaths.isEmpty || currentEntityDesc.typeUri != context.value().configuredSchema.typeUri) {
      // Retrieve the data sources
      val inputTaskId = transformTask.data.selection.inputId
      val paths = retrievePathsOfInput(inputTaskId, Some(transform.selection), transformTask, context)
      val configuredEntitySchema = currentEntityDesc.copy(typedPaths = (currentEntityDesc.typedPaths ++ paths).distinct)
      // Retrieve untyped paths if input is an RDF data source and configured type is non empty
      val isRdfInput = transformTask.project.anyTask(inputTaskId).data.isInstanceOf[RdfDataset]
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
}

/**
  * The cached schemata of the input tasks
  * @param configuredSchema The schema of the input as configured in this transform spec
  * @param untypedSchema    The optional schema of the input without type. This is stored for some datasets, currently
  *                         only RDF datasets, in order to make services like auto-completion work in hierarchical mappings.
  */
case class CachedEntitySchemata(configuredSchema: EntitySchema, untypedSchema: Option[EntitySchema]) {
  /**
    * Returns the cached paths. Depending on the provided context either the configured or the untyped
    * cached paths are returned.
    * @param task       The transform task for which the paths are requested
    * @param sourcePath The complete source path at which position the paths are requested, e.g. for auto-completion
    * @return
    */
  def fetchCachedPaths(task: ProjectTask[TransformSpec], sourcePath: List[PathOperator]): IndexedSeq[TypedPath] = {
    if(task.selection.typeUri.uri.nonEmpty && sourcePath.nonEmpty && isRdfInput(task) && untypedSchema.isDefined) {
      untypedSchema.get.typedPaths
    } else {
      configuredSchema.typedPaths
    }
  }

  def isRdfInput(task: ProjectTask[TransformSpec]): Boolean = task.project.anyTask(task.selection.inputId).data.isInstanceOf[RdfDataset]
}

object CachedEntitySchemata {
  implicit object CachedEntitySchemaXmlFormat extends XmlFormat[CachedEntitySchemata] {
    override def read(value: Node)(implicit readContext: ReadContext): CachedEntitySchemata = {
      val configured = XmlSerialization.fromXml[EntitySchema]((value \ "ConfiguredEntitySchema" \ "EntityDescription").head)
      val untyped = (value \ "UnTypedEntitySchema" \ "EntityDescription").headOption.map(XmlSerialization.fromXml[EntitySchema])
      CachedEntitySchemata(configured, untyped)
    }

    override def write(value: CachedEntitySchemata)(implicit writeContext: WriteContext[Node]): Node = {
      <CachedEntitySchemata>
        <ConfiguredEntitySchema>
          {XmlSerialization.toXml(value.configuredSchema)}
        </ConfiguredEntitySchema>
        { value.untypedSchema match {
            case Some(schema) =>
              <UnTypedEntitySchema>
                {XmlSerialization.toXml(schema)}
              </UnTypedEntitySchema>
            case None =>
              Null
          }
        }
      </CachedEntitySchemata>
    }
  }
}