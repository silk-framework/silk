/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.dataset

import java.util.logging.Logger

import org.silkframework.config.Task.TaskFormat
import org.silkframework.config.{MetaData, Prefixes, Task, TaskSpec}
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.{Resource, ResourceManager}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{Identifier, Uri}

import scala.language.implicitConversions
import scala.xml.Node

/**
  * A dataset of entities.
  *
  * @param uriProperty Setting this URI will generate an additional property for each entity.
                       The additional property contains the URI of each entity.
  */
case class DatasetSpec[+DatasetType <: Dataset](plugin: DatasetType, uriProperty: Option[Uri] = None) extends TaskSpec with DatasetAccess {

  def source(implicit userContext: UserContext): DataSource = DatasetSpec.DataSourceWrapper(plugin.source, this)

  def entitySink(implicit userContext: UserContext): EntitySink = DatasetSpec.EntitySinkWrapper(plugin.entitySink, this)

  def linkSink(implicit userContext: UserContext): LinkSink = DatasetSpec.LinkSinkWrapper(plugin.linkSink, this)

  /** Datasets don't define input schemata, because any data can be written to them. */
  override lazy val inputSchemataOpt: Option[Seq[EntitySchema]] = None

  /** Datasets don't have a static EntitySchema. It is defined by the following task. */
  override lazy val outputSchemaOpt: Option[EntitySchema] = None

  /** The resources that are referenced by this dataset. */
  override def referencedResources: Seq[Resource] = plugin.referencedResources

  /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
  override def properties(implicit prefixes: Prefixes): Seq[(String, String)] = {
    var properties =
      plugin match {
        case Dataset(p, params) =>
          Seq(("type", p.label)) ++ params
      }
    for(uriProperty <- uriProperty) {
      properties :+= ("URI Property", uriProperty.uri)
    }
    properties
  }

  override def withProperties(updatedProperties: Map[String, String])(implicit prefixes: Prefixes, resourceManager: ResourceManager): DatasetSpec[DatasetType] = {
    copy(plugin = plugin.withParameters(updatedProperties))
  }

  override def toString: String = DatasetSpec.toString

}

case class DatasetTask(id: Identifier, data: DatasetSpec[Dataset], metaData: MetaData = MetaData.empty) extends Task[DatasetSpec[Dataset]]

object DatasetSpec {

  /**
    * A DatasetSpec that is agnostic of the actual Dataset type.
    */
  type GenericDatasetSpec = DatasetSpec[Dataset]

  implicit def toTransformTask(task: Task[DatasetSpec[Dataset]]): DatasetTask = DatasetTask(task.id, task.data, task.metaData)

  def empty: DatasetSpec[EmptyDataset.type] = new DatasetSpec(EmptyDataset)

  case class DataSourceWrapper(source: DataSource, datasetSpec: DatasetSpec[Dataset]) extends DataSource {

    override def retrieveTypes(limit: Option[Int] = None)
                              (implicit userContext: UserContext): Traversable[(String, Double)] = {
      source.retrieveTypes(limit)
    }

    override def retrievePaths(typeUri: Uri, depth: Int = 1, limit: Option[Int] = None)
                              (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
      source.retrievePaths(typeUri, depth, limit)
    }

    /**
      * Retrieves entities from this source which satisfy a specific entity schema.
      *
      * @param entitySchema The entity schema
      * @param limit        Limits the maximum number of retrieved entities
      * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
      */
    override def retrieve(entitySchema: EntitySchema, limit: Option[Int])
                         (implicit userContext: UserContext): Traversable[Entity] = {
      val adaptedSchema = adaptSchema(entitySchema)
      val entities = source.retrieve(adaptedSchema, limit)
      adaptUris(entities, adaptedSchema)
    }

    /**
      * Retrieves a list of entities from this source.
      *
      * @param entitySchema The entity schema
      * @param entities     The URIs of the entities to be retrieved.
      * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
      */
    override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                              (implicit userContext: UserContext): Traversable[Entity] = {
      if(entities.isEmpty) {
        Seq.empty
      } else {
        val adaptedSchema = adaptSchema(entitySchema)
        val retrievedEntities = source.retrieveByUri(adaptedSchema, entities)
        adaptUris(retrievedEntities, adaptedSchema)
      }
    }

    /**
      * Adds the URI property to the schema.
      */
    private def adaptSchema(entitySchema: EntitySchema): EntitySchema = {
      datasetSpec.uriProperty match {
        case Some(property) =>
          entitySchema.copy(typedPaths = entitySchema.typedPaths :+ TypedPath(UntypedPath.parse(property.uri), StringValueType, isAttribute = false)) // StringValueType since UriType will often fail URI validation resulting in failed entities
        case None =>
          entitySchema
      }
    }

    /**
      * Rewrites the entity URIs if an URI property has been specified.
      */
    private def adaptUris(entities: Traversable[Entity], entitySchema: EntitySchema): Traversable[Entity] = {
      datasetSpec.uriProperty match {
        case Some(property) =>
          for (entity <- entities) yield {
            Entity(
              uri = new Uri(entity.singleValue(TypedPath(UntypedPath.parse(property.uri), StringValueType, isAttribute = false)).getOrElse(entity.uri.toString)),
              values = entity.values,
              schema = entity.schema
            )
          }
        case None =>
          entities
      }
    }

    /**
      * The dataset task underlying the Datset this source belongs to
      *
      * @return
      */
    override def underlyingTask: Task[DatasetSpec[Dataset]] = source.underlyingTask
  }

  case class EntitySinkWrapper(entitySink: EntitySink, datasetSpec: DatasetSpec[Dataset]) extends EntitySink {

    private val log = Logger.getLogger(DatasetSpec.getClass.getName)

    private var isOpen = false

    /**
      * Initializes this writer.
      */
    override def openTable(typeUri: Uri, properties: Seq[TypedProperty])
                          (implicit userContext: UserContext, prefixes: Prefixes){
      if (isOpen) {
        entitySink.close()
        isOpen = false
      }
      val uriTypedProperty = datasetSpec.uriProperty.map(p => TypedProperty(p.uri, StringValueType, isBackwardProperty = false))
      entitySink.openTable(typeUri, uriTypedProperty.toIndexedSeq ++ properties)
      isOpen = true
    }


    override def openTableWithPaths(typeUri: Uri, typedPaths: Seq[TypedPath])(implicit userContext: UserContext, prefixes: Prefixes): Unit = {
      if (isOpen) {
        entitySink.close()
        isOpen = false
      }
      val uriTypedProperty = datasetSpec.uriProperty.map(p => TypedPath(p.uri, StringValueType))
      entitySink.openTableWithPaths(typeUri, uriTypedProperty.toIndexedSeq ++ typedPaths)
      isOpen = true

    }

    /**
      * Called before a new table of entities of a particular schema is written.
      */
    override def openWithEntitySchema(es: EntitySchema)(implicit userContext: UserContext, prefixes: Prefixes): Unit = {
      if (isOpen) {
        entitySink.close()
        isOpen = false
      }
      val uriTypedProperty = datasetSpec.uriProperty.map(p => TypedPath(p.uri, StringValueType))
      entitySink.openWithEntitySchema(es.copy(typedPaths = uriTypedProperty.toIndexedSeq ++ es.typedPaths))
      isOpen = true
    }

    override def writeEntity(subject: String, values: Seq[Seq[String]])
                            (implicit userContext: UserContext): Unit = {
      require(isOpen, "Output must be opened before writing statements to it")
      datasetSpec.uriProperty match {
        case Some(_) =>
          entitySink.writeEntity(subject, Seq(subject) +: values)
        case None =>
          entitySink.writeEntity(subject, values)
      }
    }

    /**
      * Writes a new entity.
      *
      * @param entity - the entity to write
      */
    override def writeEntity(entity: Entity)(implicit userContext: UserContext): Unit = {
      require(isOpen, "Output must be opened before writing statements to it")
      datasetSpec.uriProperty match {
        case Some(_) =>
          val uriTypedProperty = datasetSpec.uriProperty.map(p => TypedPath(p.uri, StringValueType))
          val schema = entity.schema.copy(typedPaths = uriTypedProperty.toIndexedSeq ++ entity.schema.typedPaths)
          entitySink.writeEntity(entity.copy(values = IndexedSeq(Seq(entity.uri.toString)) ++ entity.values, schema = schema))
        case None =>
          entitySink.writeEntity(entity)
      }
    }

    /**
      * Write a complete table based on the provided collection of Entities
      */
    override def writeEntities(entities: Traversable[Entity])(implicit userContext: UserContext, prefixes: Prefixes): Unit = {
      entities.headOption match{
        case Some(h) =>
          openWithEntitySchema(h.schema)
          val uriTypedProperty = datasetSpec.uriProperty.map(p => TypedPath(p.uri, StringValueType))
          val schema = h.schema.copy(typedPaths = uriTypedProperty.toIndexedSeq ++ h.schema.typedPaths)
          entities.foreach(e => entitySink.writeEntity(e.copy(values = IndexedSeq(Seq(e.uri.toString)) ++ e.values, schema = schema)))
          closeTable()
        case None =>
      }
    }

    /**
      * Closes the current table.
      */
    override def closeTable()(implicit userContext: UserContext) {
      if (entitySink != null) entitySink.closeTable()
      isOpen = false
    }

    /**
      * Closes this writer.
      */
    override def close()(implicit userContext: UserContext) {
      if (entitySink != null) entitySink.close()
      isOpen = false
    }

    /**
      * Makes sure that the next write will start from an empty dataset.
      */
    override def clear()(implicit userContext: UserContext): Unit = entitySink.clear()
  }

  case class LinkSinkWrapper(linkSink: LinkSink, datasetSpec: DatasetSpec[Dataset]) extends LinkSink {

    private val log = Logger.getLogger(DatasetSpec.getClass.getName)

    private var linkCount: Int = 0

    private var isOpen = false

    override def init()(implicit userContext: UserContext): Unit = {
      if (isOpen) {
        linkSink.close()
        isOpen = false
      }
      linkSink.init()
      isOpen = true
    }

    /**
      * Writes a new link to this writer.
      */
    override def writeLink(link: Link, predicateUri: String)
                          (implicit userContext: UserContext): Unit = {
      //require(isOpen, "Output must be opened before writing statements to it")

      linkSink.writeLink(link, predicateUri)
      linkCount += 1
    }

    /**
      * Closes this writer.
      */
    override def close()(implicit userContext: UserContext) {
      if (linkSink != null) linkSink.close()
      isOpen = false
      log.info(s"Wrote $linkCount links.")
    }

    /**
      * Makes sure that the next write will start from an empty dataset.
      */
    override def clear()(implicit userContext: UserContext): Unit = linkSink.clear()
  }

  /**
    * XML serialization format.
    */
  implicit object DatasetSpecFormat extends XmlFormat[DatasetSpec[Dataset]] {

    override def tagNames: Set[String] = Set("Dataset")

    def read(node: Node)(implicit readContext: ReadContext): DatasetSpec[Dataset] = {
      implicit val prefixes: Prefixes = readContext.prefixes
      implicit val resources: ResourceManager = readContext.resources

      // Check if the data source still uses the old outdated XML format
      if (node.label == "DataSource" || node.label == "Output") {
        // Read old format
        val id = (node \ "@id").text
        new DatasetSpec(
          plugin = Dataset((node \ "@type").text, XmlSerialization.deserializeParameters(node))
        )
      } else {
        // Read new format
        val id = (node \ "@id").text
        val uriProperty = (node \ "@uriProperty").headOption.map(_.text).filter(_.trim.nonEmpty).map(Uri(_))
        // In outdated formats the plugin parameters are nested inside a DatasetPlugin node
        val sourceNode = (node \ "DatasetPlugin").headOption.getOrElse(node)
        new DatasetSpec(
          plugin = Dataset((sourceNode \ "@type").text, XmlSerialization.deserializeParameters(sourceNode)),
          uriProperty = uriProperty
        )
      }
    }

    def write(value: DatasetSpec[Dataset])(implicit writeContext: WriteContext[Node]): Node = {
      value.plugin match {
        case Dataset(pluginDesc, params) =>
          <Dataset type={pluginDesc.id} uriProperty={value.uriProperty.map(_.uri).getOrElse("")}>
            {XmlSerialization.serializeParameter(params)}
          </Dataset>
      }
    }
  }

  implicit object DatasetTaskXmlFormat extends XmlFormat[DatasetTask] {
    override def read(value: Node)(implicit readContext: ReadContext): DatasetTask = {
      new TaskFormat[DatasetSpec[Dataset]].read(value)
    }

    override def write(value: DatasetTask)(implicit writeContext: WriteContext[Node]): Node = {
      new TaskFormat[DatasetSpec[Dataset]].write(value)
    }
  }

}