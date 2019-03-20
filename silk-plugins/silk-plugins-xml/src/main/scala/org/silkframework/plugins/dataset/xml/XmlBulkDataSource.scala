package org.silkframework.plugins.dataset.xml

import java.util.logging.{Level, Logger}

import org.silkframework.dataset._
import org.silkframework.entity._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.BulkResource
import org.silkframework.util.Uri

import scala.collection.mutable
import scala.xml.XML

//FIXME: Make more generic, e.g. for all resource based data sets if dependencies and other traits allow it

/**
  * Wrapper for a data source object based on streaming data source.*
  */
class XmlBulkDataSource(bulk: BulkResource, base: String, pattern: String) extends XmlSourceStreaming(bulk, base, pattern) with PeakDataSource with TypedPathRetrieveDataSource {

  private var underlyingDataset: Option[Dataset] = None

  /**
    * Create streaming based data source that uses a bulk resource.
    *
    * @param bulk         Zip Resource
    * @param base         Base path
    * @param pattern      Uri pattern
    * @param dataset      DataSet
    * @return             DataSource object
    */
  def apply(bulk: BulkResource, base: String, pattern: String, dataset: Dataset): DataSource = {
    underlyingDataset = Some(dataset)
    new XmlBulkDataSource(bulk, base, pattern)
  }

  /**
    * Get source for individual sub resources.
    *
    * @return Set of sources
    */
  def individualSources: Seq[XmlSourceStreaming] = for (stream <- bulk.inputStreams) yield {
    val subResource = BulkResource.createBulkResourceWithStream(bulk, stream)
    new XmlSourceStreaming(subResource, base, pattern)
  }

  /**
    * Override because of collecting types from all sources
    */
  override def retrieveTypes(limit: Option[Int])
                            (implicit userContext: UserContext): Traversable[(String, Double)] = {

    val types: mutable.HashSet[(String, Double)] = new mutable.HashSet[(String, Double)]
    for (source <- individualSources) {
      val subResourceTypes: Traversable[(String, Double)] = source.retrieveTypes(limit)
      subResourceTypes.foreach(t => types.add(t))
    }
    types
  }

  /**
    * Override because of collecting types from all sources
    */
  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])
                            (implicit userContext: UserContext): IndexedSeq[Path] = {
    val paths: mutable.HashSet[TypedPath] = new mutable.HashSet[TypedPath]
    for (source <- individualSources) {
      val subResourcePaths: IndexedSeq[TypedPath] = source.retrieveXmlPaths(
        typeUri, depth, limit, onlyLeafNodes = false, onlyInnerNodes = false
      ).drop(1)

      subResourcePaths.foreach(p => paths.add(p))
    }
    paths.map(tp => Path(tp.operators)).toIndexedSeq
  }

  /**
    * Override because of collecting types from all sources
    */
  override def retrieveTypedPath(typeUri: Uri,
                                 depth: Int,
                                 limit: Option[Int])
                                (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    val paths: mutable.HashSet[TypedPath] = new mutable.HashSet[TypedPath]
    for (source <- individualSources) {
      val subResourcePaths: IndexedSeq[TypedPath] = source.retrieveXmlPaths(
        typeUri, depth, limit, onlyLeafNodes = false, onlyInnerNodes = false
      ).drop(1)
      subResourcePaths.foreach(p => paths.add(p))
    }
    paths.toIndexedSeq
  }

}

/**
  * Wrapper for a data source object based on in memory data source.*
  */
class XmlBulkDataSourceInMemory(bulk: BulkResource, base: String, pattern: String)
  extends XmlSourceInMemory(bulk, base, pattern) with PeakDataSource with TypedPathRetrieveDataSource {

  private final val logger: Logger = Logger.getLogger(this.getClass.getSimpleName)

  private var underlyingDataset: Option[Dataset] = None

  /**
    * Create streaming based data source that uses a bulk resource.
    *
    * @param bulk         Zip Resource
    * @param base         Base path
    * @param pattern      Uri pattern
    * @param dataset      DataSet
    * @return             DataSource object
    */
  def apply(bulk: BulkResource, base: String, pattern: String, dataset: Dataset): DataSource = {
    underlyingDataset = Some(dataset)
    new XmlBulkDataSource(bulk, base, pattern)
  }

  /**
    * Get source for individual sub resources.
    *
    * @return Set of sources
    */
  def  individualSources: Seq[XmlSourceInMemory] = for (stream <- bulk.inputStreams) yield {
    val subResource = BulkResource.createBulkResourceWithStream(bulk, stream)
    new XmlSourceInMemory(subResource, base, pattern)
  }

  /**
    * Override because of collecting types from all sources
    */
  override def retrieveTypes(limit: Option[Int])
                            (implicit userContext: UserContext): Traversable[(String, Double)] = {

    val types: mutable.HashSet[(String, Double)] = new mutable.HashSet[(String, Double)]
    for (source <- individualSources) {
      val subResourceTypes: Traversable[(String, Double)] = source.retrieveTypes(limit)
      subResourceTypes.foreach(t => types.add(t))
    }
    types
  }

  /**
    * Override because of collecting types from all sources
    */
  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])
                            (implicit userContext: UserContext): IndexedSeq[Path] = {
    val paths: mutable.HashSet[TypedPath] = new mutable.HashSet[TypedPath]
    for (source <- individualSources) {
      val subResourcePaths: IndexedSeq[TypedPath] = source.retrieveXmlPaths(
        typeUri, depth, limit, onlyLeafNodes = false, onlyInnerNodes = false
      ).drop(1)

      subResourcePaths.foreach(p => paths.add(p))
    }
    paths.map(tp => Path(tp.operators)).toIndexedSeq
  }

  /**
    * Override because of collecting types from all sources
    */
  override def retrieveTypedPath(typeUri: Uri,
                                 depth: Int,
                                 limit: Option[Int])
                                (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    val paths: mutable.HashSet[TypedPath] = new mutable.HashSet[TypedPath]
    for (source <- individualSources) {
      val subResourcePaths: IndexedSeq[TypedPath] = source.retrieveXmlPaths(
        typeUri, depth, limit, onlyLeafNodes = false, onlyInnerNodes = false
      ).drop(1)
      subResourcePaths.foreach(p => paths.add(p))
    }
    paths.toIndexedSeq
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


  private def loadXmlNodes(typeUri: String): Seq[XmlTraverser] = {
    // If a type URI is provided, we use it as path. Otherwise we are using the base Path (which is deprecated)
    val pathStr = if (typeUri.isEmpty) base else typeUri
    val xml = bulk.read(XML.load)
    val rootTraverser = XmlTraverser(xml)
    // Move to base path
    rootTraverser.evaluatePath(Path.parse(pathStr))
  }

  private class Entities(xml: Seq[XmlTraverser], entityDesc: EntitySchema) extends Traversable[Entity] {
    def foreach[U](f: Entity => U) {
      // Enumerate entities
      for ((traverser, index) <- xml.zipWithIndex) {
        val uri = traverser.generateUri(pattern)
        val values = for (typedPath <- entityDesc.typedPaths) yield traverser.evaluatePathAsString(typedPath, pattern)
        f(Entity(uri, values, entityDesc))
      }
    }
  }

  override def combinedPath(typeUri: String, inputPath: Path): Path = {
    val typePath = Path.parse(typeUri)
    Path(typePath.operators ++ inputPath.operators)
  }

  override def convertToIdPath(path: Path): Option[Path] = {
    Some(Path(path.operators ::: List(ForwardOperator("#id"))))
  }

  override def peak(entitySchema: EntitySchema, limit: Int)
                   (implicit userContext: UserContext): Traversable[Entity] = {
    peakWithMaximumFileSize(bulk, entitySchema, limit)
  }

  override def collectPaths(limit: Int, collectValues: (List[String], String) => Unit): Seq[List[String]] = {
    // Re-use implementation of streaming based XML source
    new XmlSourceStreaming(bulk, base, pattern).collectPaths(limit, collectValues)
  }
}
