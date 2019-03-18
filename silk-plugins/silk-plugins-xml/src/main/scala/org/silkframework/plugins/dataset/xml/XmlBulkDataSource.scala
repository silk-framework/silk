package org.silkframework.plugins.dataset.xml

import java.util.logging.{Level, Logger}

import org.silkframework.dataset._
import org.silkframework.entity.{Path, TypedPath}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.BulkResource
import org.silkframework.util.Uri

import scala.collection.mutable

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
}
