package org.silkframework.dataset.bulk

import org.silkframework.dataset.{DataSource, Dataset, ResourceBasedDataset, TypedPathRetrieveDataSource}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{BulkResource, Resource}
import org.silkframework.runtime.validation.ValidationException

/**
  * Utilities to replace the data source object with a source based on bulk resources.
  * a potential bulk resource supporting data set
  *
  * @see ResourceBasedDataset
  */
trait BulkResourceBasedDataset extends ResourceBasedDataset { this: Dataset =>

  /** If true, the types and paths of the underlying data sources are merged.
    * If false, the types and paths of the first data source are used.
    */
  def mergeSchemata: Boolean

  /**
    * Create a data source for a particular resource inside the bulk file.
    */
  def createSource(resource: Resource): DataSource with TypedPathRetrieveDataSource

  /**
    * Returns a data source for reading entities from the data set.
    */
  override final def source(implicit userContext: UserContext): DataSource with TypedPathRetrieveDataSource = {
    bulkFile() match {
      case Some(bulk) =>
        new BulkDataSource(bulk.subResources.map(createSource), mergeSchemata)
      case None =>
        createSource(file)
    }
  }

  /**
    * If this is a bulk file, returns the first sub resource.
    * Otherwise, returns the file itself.
    *
    * @throws ValidationException If this is a bulk file and the bulk file is empty.
    */
  def firstResource: Resource = {
    bulkFile() match {
      case Some(r) =>
        r.subResources.headOption.getOrElse(throw new ValidationException(s"Bulk file ${r.name} is empty"))
      case None =>
        file
    }
  }


  /**
    * Get an instance of a BulkResource, if a bulk file is given. Otherwise returns None.
    * Some functions expect the file ending of the bulk content instead of ".zip". Optionally a virtual file ending for
    * the zip can be provided.
    */
  def bulkFile(): Option[BulkResource] = {
    if (BulkResource.isBulkResource(file)) {
      Some(BulkResource.asBulkResource(file))
    } else {
      None
    }
  }

}
