package org.silkframework.dataset.bulk

import java.io.File
import java.util.logging.Logger

import org.silkframework.dataset.{DataSource, Dataset, ResourceBasedDataset, TypedPathRetrieveDataSource}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{BulkResource, Resource, WritableResource}

/**
  * Utilities to replace the data source object with a source based on bulk resources.
  * a potential bulk resource supporting data set
  *
  * @see ResourceBasedDataset
  */
trait BulkResourceBasedDataset extends ResourceBasedDataset { this: Dataset =>

  private final val log: Logger = Logger.getLogger(this.getClass.getSimpleName)

  /** If true, the types and paths of the underlying data sources are merged.
    * If false, the types and paths of the first data source are used.
    */
  def mergeSchemata: Boolean

  /**
    * Create a data source for a particular resource inside the bulk file.
    */
  def createSource(resource: Resource): DataSource with TypedPathRetrieveDataSource

  override final def source(implicit userContext: UserContext): DataSource with TypedPathRetrieveDataSource = {
    bulkFile() match {
      case Some(bulk) =>
        new BulkDataSource(bulk.subResources.map(createSource), mergeSchemata)
      case None =>
        createSource(file)
    }
  }


  /**
    * Get an instance of a BulkResource, if a bulk file is given. Otherwiaw returns None.
    * Some fuctions expect the file ending of the bulk content instead of ".zip". Optionally a virtual file ending for
    * the zip can be provided.
    *
    * @param virtualEnding
    * @return
    */
  def bulkFile(virtualEnding: Option[String] = None): Option[BulkResource] = {
    if (isBulkResource(file)) {
      Some(asBulkResource(file, virtualEnding))
    }
    else {
      None
    }
  }


  /**
    * Returns true if the given resource is a BulkResource and false otherwise.
    * A BulkResource is detected if the file belonging to the given resource ends with .zip or is a
    * directory.
    *
    * @param resource WritableResource to check
    * @return true if an archive or folder
    */
  protected def isBulkResource(resource: Resource): Boolean = {
    resource.name.endsWith(".zip") && !new File(resource.path).isDirectory
  }

  /**
    * Returns a BulkResource depending on the given inputs location and name.
    * A BulkResource is returned if the file belonging to the given resource ends with .zip or is a
    * directory.
    *
    * @param resource WritableResource tha may be zip or folder
    * @return instance of BulkResource
    */
  protected def asBulkResource(resource: Resource, virtualEnding: Option[String] = None): BulkResource = {
    if (resource.name.endsWith(".zip") && !new File(resource.path).isDirectory) {
      log info s"Zip file Resource found: ${resource.name}"
      BulkResource(new File(resource.path), virtualEnding)
    }
    else if (new File(resource.path).isDirectory) {
      log info s"Resource Folder found: ${resource.name}"
      throw new NotImplementedError("The bulk resource support does not work for non-zip files for now")    }
    else {
      throw new IllegalArgumentException(resource.path + " is not a bulk resource.")
    }
  }

}
