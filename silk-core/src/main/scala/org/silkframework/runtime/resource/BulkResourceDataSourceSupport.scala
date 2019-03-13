package org.silkframework.runtime.resource

import java.util.logging.Level

import org.silkframework.config.{PlainTask, Task, TaskSpec}
import org.silkframework.dataset._
import org.silkframework.entity.{Entity, EntitySchema, Path, TypedPath}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.BulkResourceSupport.getDistinctSchemaDescriptions
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import scala.xml.XML

object BulkResourceDataSource extends DataSource with PeakDataSource with TypedPathRetrieveDataSource {

  var underlyingResource: Option[BulkResource] = None
  var underlyingDataset: Option[XmlDataset] = None
  var underlyingSource = Option[DataSource] = None
  var subSources = Seq[DataSource]
  var streaming: Option[Boolean] = Nonev
  var individualSources: Seq[DataSource]

  def apply(bulkResource: BulkResource, dataset: Dataset, dataSource: DataSource, isStreamin: Boolean): DataSource = {
    underlyingDataset = Some(dataset)
    underlyingResource = Some(bulkResource)
    underlyingSource = Some(dataSource)
    streaming = Some(isStreamin)
    this
  }

  /**
    * The dataset task underlying the Datset this source belongs to
    *
    * @return
    */
  override def underlyingTask: Task[DatasetSpec[Dataset]] = underlyingDataset.map(ds => {
    PlainTask(ds.pluginSpec.id,new DatasetSpec(ds))
  }).getOrElse(throw new RuntimeException("The underlying data source for the BulkResourceDataSource is missing."))

  /**
    * Retrieves known types in this source.
    * Implementations are only required to work on a best effort basis i.e. it does not necessarily return any or all types.
    * The default implementation returns an empty traversable.
    *
    * @param limit Restricts the number of types to be retrieved. If not given, all found types are returned.
    *
    */
  override def retrieveTypes(limit: Option[Int])(implicit userContext: UserContext): Traversable[(String, Double)] = {

    val individualSources = for (stream <- underlyingResource.get.inputStreams) yield {
      BulkResource.createFromBulkResource(bulkResource, stream)
    }

    val baePAth =underlyingResource.get.base

    val individualSchemata: IndexedSeq[EntitySchema] = individualSources.map( res => {
      val xmlSource = if(streaming.nonEmpty && streaming.get.equals(true)) {
        new XmlSourceStreaming(res, underlyingDataset.get.b, uriPattern)
      }
      else {
        new XmlSourceInMemory(res, basePath, uriPattern)
      }

      implicit val userContext: UserContext = UserContext.INTERNAL_USER
      val typeUri = xmlSource.retrieveTypes()
      val typedPaths = xmlSource.retrieveTypedPath("")
      EntitySchema(typeUri.head._1, typedPaths)

    }).toIndexedSeq

    getDistinctSchemaDescriptions(individualSchemata)
  }

  def retrieveTypes(bulkResourceDataSource: BulkResource)

  override def retrieveTypedPath(typeUri: Uri,
                                 depth: Int,
                                 limit: Option[Int])
                                (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    retrieveXmlPaths(typeUri, depth, limit, onlyLeafNodes = false, onlyInnerNodes = false)
  }






  override def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None)
                       (implicit userContext: UserContext): Traversable[Entity] = {
    logger.log(Level.FINE, "Retrieving data from XML.")

    val nodes = loadXmlNodes(entitySchema.typeUri.uri)
    val subTypeEntities = if(entitySchema.subPath.operators.nonEmpty) {
      nodes.flatMap(_.evaluatePath(entitySchema.subPath))
    } else { nodes }
    val entities = new Entities(subTypeEntities, entitySchema)

    limit match {
      case Some(max) => entities.take(max)
      case None => entities
    }
  }

  /**
    * Returns the XML nodes found at the base path and
    *
    * @return
    */
  private def loadXmlNodes(typeUri: String): Seq[XmlTraverser] = {
    // If a type URI is provided, we use it as path. Otherwise we are using the base Path (which is deprecated)
    val pathStr = if (typeUri.isEmpty) basePath else typeUri
    // Load XML
    val xml = file.read(XML.load)
    val rootTraverser = XmlTraverser(xml)
    // Move to base path
    rootTraverser.evaluatePath(Path.parse(pathStr))
  }

  private class Entities(xml: Seq[XmlTraverser], entityDesc: EntitySchema) extends Traversable[Entity] {
    def foreach[U](f: Entity => U) {
      // Enumerate entities
      for ((traverser, index) <- xml.zipWithIndex) {
        val uri = traverser.generateUri(uriPattern)
        val values = for (typedPath <- entityDesc.typedPaths) yield traverser.evaluatePathAsString(typedPath, uriPattern)
        f(Entity(uri, values, entityDesc))
      }
    }
  }


  /**
    * Retrieves the most frequent paths in this source.
    * Implementations are only required to work on a best effort basis i.e. it does not necessarily return all paths in the source.
    * The default implementation returns an empty traversable.
    *
    * @param typeUri The entity type for which paths shall be retrieved
    * @param depth   Only retrieve paths up to a certain length. If not given, only paths of length 1 are returned. Since
    *                this value can be set to Int.MaxValue, the source has to make sure that it returns a result that
    *                can still be handled, e.g. it is Ok for XML and JSON to return all paths, for GRAPH data models this
    *                would be infeasible.
    * @param limit   Restricts the number of paths to be retrieved. If not given, all found paths are returned.
    * @return A Sequence of the found paths sorted by their frequency (most frequent first).
    */
  override def retrievePaths(t: Uri, depth: Int, limit: Option[Int])
                            (implicit userContext: UserContext): IndexedSeq[Path] = {
    retrieveXmlPaths(t, depth, limit, onlyLeafNodes = false, onlyInnerNodes = false) map (tp => Path(tp.operators))
  }
  /**
    * Retrieves entities from this source which satisfy a specific entity schema.
    *
    * @param entitySchema The entity schema
    * @param limit        Limits the maximum number of retrieved entities
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieve(entitySchema: EntitySchema, limit: Option[Int])(implicit userContext: UserContext): Traversable[Entity] = ???

  /**
    * Retrieves a list of entities from this source.
    *
    * @param entitySchema The entity schema
    * @param entities     The URIs of the entities to be retrieved.
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])(implicit userContext: UserContext): Traversable[Entity] = ???
}
