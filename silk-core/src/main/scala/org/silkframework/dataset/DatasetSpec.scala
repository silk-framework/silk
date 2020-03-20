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
import org.silkframework.config._
import org.silkframework.dataset.DatasetSpec.UriAttributeNotUniqueException
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{Resource, ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.util.{Identifier, Uri}

import scala.language.implicitConversions
import scala.xml.Node

/**
  * A dataset of entities.
  *
  * @param uriAttribute Setting this URI will generate an additional attribute for each entity.
                        The additional attribute contains the URI of each entity.
  */
case class DatasetSpec[+DatasetType <: Dataset](plugin: DatasetType, uriAttribute: Option[Uri] = None) extends TaskSpec with DatasetAccess {

  def source(implicit userContext: UserContext): DataSource = {
    safeAccess(DatasetSpec.DataSourceWrapper(plugin.source, this), SafeModeDataSource)
  }

  def entitySink(implicit userContext: UserContext): EntitySink = {
    safeAccess(DatasetSpec.EntitySinkWrapper(plugin.entitySink, this), SafeModeSink)
  }

  def linkSink(implicit userContext: UserContext): LinkSink = {
    safeAccess(DatasetSpec.LinkSinkWrapper(plugin.linkSink, this), SafeModeSink)
  }

  // True if access should be prevented regarding the dataset and safe-mode config
  private def preventAccessInSafeMode(implicit userContext: UserContext): Boolean = {
    ProductionConfig.inSafeMode && !plugin.isFileResourceBased && !userContext.executionContext.insideWorkflow
  }

  // Create data access object or return fallback
  private def safeAccess[T](create: T, fallback: T)
                           (implicit userContext: UserContext): T = {
    if (preventAccessInSafeMode) {
      fallback
    } else {
      create
    }
  }

  /** Datasets don't define input schemata, because any data can be written to them. */
  override lazy val inputSchemataOpt: Option[Seq[EntitySchema]] = None

  /** Datasets don't have a static EntitySchema. It is defined by the following task. */
  override lazy val outputSchemaOpt: Option[EntitySchema] = None

  /** The resources that are referenced by this dataset. */
  override def referencedResources: Seq[Resource] = plugin.referencedResources

  /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
  override def properties(implicit prefixes: Prefixes, resourceLoader: ResourceLoader): Seq[(String, String)] = {
    var properties =
      plugin match {
        case Dataset(p, params) =>
          Seq(("type", p.label)) ++ params
      }
    for(uriProperty <- uriAttribute) {
      properties :+= ("URI Property", uriProperty.uri)
    }
    properties
  }

  override def withProperties(updatedProperties: Map[String, String])(implicit prefixes: Prefixes, resourceManager: ResourceManager): DatasetSpec[DatasetType] = {
    copy(plugin = plugin.withParameters(updatedProperties))
  }

  override def toString: String = DatasetSpec.toString

  def assertUriAttributeUniqueness(attributes: Traversable[String]): Unit = {
    for(uriColumn <- uriAttribute if attributes.exists(_ == uriColumn.uri)) {
      throw UriAttributeNotUniqueException(uriColumn)
    }
  }

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
                         (implicit userContext: UserContext): EntityHolder = {
      val adaptedSchema = adaptSchema(entitySchema)
      val entities = source.retrieve(adaptedSchema, limit)
      adaptUris(entities)
    }

    /**
      * Retrieves a list of entities from this source.
      *
      * @param entitySchema The entity schema
      * @param entities     The URIs of the entities to be retrieved.
      * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
      */
    override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                              (implicit userContext: UserContext): EntityHolder = {
      if(entities.isEmpty) {
        GenericEntityTable(Seq.empty, entitySchema, underlyingTask)
      } else {
        val adaptedSchema = adaptSchema(entitySchema)
        val retrievedEntities = source.retrieveByUri(adaptedSchema, entities)
        adaptUris(retrievedEntities)
      }
    }

    /**
      * Adds the URI property to the schema.
      */
    private def adaptSchema(entitySchema: EntitySchema): EntitySchema = {
      datasetSpec.uriAttribute match {
        case Some(property) =>
          entitySchema.copy(typedPaths = entitySchema.typedPaths :+ TypedPath(UntypedPath.parse(property.uri), ValueType.URI, isAttribute = false))
        case None =>
          entitySchema
      }
    }

    /**
      * Rewrites the entity URIs if an URI property has been specified.
      */
    private def adaptUris(entities: EntityHolder): EntityHolder = {
      datasetSpec.uriAttribute match {
        case Some(property) =>
          entities.mapEntities( entity =>
            Entity(
              uri = new Uri(entity.singleValue(TypedPath(UntypedPath.parse(property.uri), ValueType.URI, isAttribute = false)).getOrElse(entity.uri.toString)),
              values = entity.values,
              schema = entity.schema
            )
          )
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

      datasetSpec.assertUriAttributeUniqueness(properties.map(_.propertyUri))

      val uriTypedProperty =
        for(property <- datasetSpec.uriAttribute.toIndexedSeq) yield {
          TypedProperty(property.uri, ValueType.URI, isBackwardProperty = false)
        }

      entitySink.openTable(typeUri, uriTypedProperty ++ properties)
      isOpen = true
    }

    override def writeEntity(subject: String, values: Seq[Seq[String]])
                            (implicit userContext: UserContext): Unit = {
      require(isOpen, "Output must be opened before writing statements to it")
      datasetSpec.uriAttribute match {
        case Some(_) =>
          entitySink.writeEntity(subject, Seq(subject) +: values)
        case None =>
          entitySink.writeEntity(subject, values)
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
          uriAttribute = uriProperty
        )
      }
    }

    def write(value: DatasetSpec[Dataset])(implicit writeContext: WriteContext[Node]): Node = {
      value.plugin match {
        case Dataset(pluginDesc, params) =>
          <Dataset type={pluginDesc.id} uriProperty={value.uriAttribute.map(_.uri).getOrElse("")}>
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

  /**
    * Thrown if a URI attribute has been configured in DatasetSpec that is not unique.
    */
  case class UriAttributeNotUniqueException(uriColumn: String) extends Exception(
    s"Dataset is configured to add URI attribute '$uriColumn', but generated dataset already contains an attribute of that name."
  )

}