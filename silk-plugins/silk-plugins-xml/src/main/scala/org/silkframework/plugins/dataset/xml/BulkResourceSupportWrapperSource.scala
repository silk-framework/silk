package org.silkframework.plugins.dataset.xml

import java.util.logging.{Level, Logger}

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset._
import org.silkframework.entity.{Entity, EntitySchema, Path, TypedPath}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.BulkResource
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import scala.collection.mutable
import scala.xml.XML

object BulkResourceSupportWrapperSource extends DataSource with PeakDataSource with TypedPathRetrieveDataSource {

  private final val logger: Logger = Logger.getLogger(this.getClass.getSimpleName)

  private var underlyingResource: Option[BulkResource] = None
  private var underlyingDataset: Option[XmlDataset] = None
  private var underlyingSource: Option[DataSource] = None
  private var streaming: Option[Boolean] = None
  private var individualSources: Seq[DataSource] = Seq.empty
  private val basePath: String = underlyingDataset.get.basePath
  private val uriPattern: String = underlyingDataset.get.uriPattern

  def apply(bulkResource: BulkResource, dataset: XmlDataset, dataSource: DataSource, isStreamin: Boolean): DataSource = {
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
    PlainTask(ds.pluginSpec.id, new DatasetSpec(ds))
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
      val subResource = BulkResource.createFromBulkResource(underlyingResource.get, stream)
      if (streaming.nonEmpty && streaming.get.equals(true)) {
        new XmlSourceStreaming(subResource, basePath, uriPattern)
      }
      else {
        new XmlSourceInMemory(subResource, basePath, uriPattern)
      }
    }
    val types: mutable.HashSet[(String, Double)] = new mutable.HashSet[(String, Double)]

    for (source <- individualSources) {
      val subResourceTypes: Traversable[(String, Double)] = source.retrieveTypes(limit)
      subResourceTypes.foreach(t => types.add(t))
    }
    types
  }


  /**
    * Returns the XML nodes found at the base path and
    *
    * @return
    */
  private def loadXmlNodes(typeUri: String): Seq[XmlTraverser] = {
    val pathStr = if (typeUri.isEmpty) basePath else typeUri
    val traverserSet = for (sub <- underlyingResource.get.subResources) yield {
      val xml = sub.read(XML.load)
      val rootTraverser = XmlTraverser(xml)
      rootTraverser.evaluatePath(Path.parse(pathStr))
    }
    traverserSet.flatten
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
    * @param t The entity type for which paths shall be retrieved
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

  private def retrieveXmlPaths(typeUri: Uri, depth: Int, limit: Option[Int], onlyLeafNodes: Boolean, onlyInnerNodes: Boolean): IndexedSeq[TypedPath] = {
    // At the moment we just generate paths from the first xml node that is found
    val xml = loadXmlNodes(typeUri.uri)
    if (xml.isEmpty) {
      throw new ValidationException(
        s"There are no XML nodes at the given path ${typeUri.toString} in resource ${underlyingResource.get.name}"
      )
    } else {
      xml.head.collectPaths(onlyLeafNodes = onlyLeafNodes, onlyInnerNodes = onlyInnerNodes, depth).toIndexedSeq
    }
  }

  /**
    * Retrieves entities from this source which satisfy a specific entity schema.
    *
    * @param entitySchema The entity schema
    * @param limit        Limits the maximum number of retrieved entities
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
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
    * Retrieves a list of entities from this source.
    *
    * @param entitySchema The entity schema
    * @param entities     The URIs of the entities to be retrieved.
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])(implicit userContext: UserContext): Traversable[Entity] = {
    throw new NotImplementedError()
  }

  /**
    * Retrieves typed paths. The value type of the path denotes what type this path has in the corresponding data source.
    * The [[org.silkframework.entity.UriValueType]] has a special meaning for non-RDF data sources, in that it specifies
    * non-literal values, e.g. a XML element with nested elements, a JSON object or array of objects etc.
    *
    * @param typeUri The type URI. For non-RDF data types this is not a URI, e.g. XML or JSON this may express the path from the root.
    * @param depth   The maximum depths of the returned paths. This is only a limit, but not a guarantee that all paths
    *                of this length are actually returned.
    * @param limit   The maximum number of typed paths returned. None stands for unlimited.
    */
  override def retrieveTypedPath(typeUri: Uri, depth: Int, limit: Option[Int])
                                (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    retrieveXmlPaths(typeUri, depth, limit, onlyLeafNodes = false, onlyInnerNodes = false)
  }}
