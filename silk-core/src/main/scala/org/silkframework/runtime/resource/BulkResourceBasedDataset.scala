package org.silkframework.runtime.resource

import java.io.File
import java.util.logging.Logger

import org.silkframework.dataset.Dataset

/**
  * Utilities to replace the data source object with a source based on bulk resources.
  * This also would replace EesourceBasedDataset. At least every resource based dataset is
  * a potential bulk resource supporting data set
  *
  * @see ResourceBasedDataset
  */
// FIXME: Remove or extend ResourceBasedDataset after bulk support is there for all relevant datasets
trait BulkResourceBasedDataset  { this: Dataset =>

  private final val log: Logger = Logger.getLogger(this.getClass.getSimpleName)

  /** The resource the dataset is reading from */
  def file: Resource

  def writableResource: Option[WritableResource] = file match {
    case wr: WritableResource => Some(wr)
    case _ => None
  }

  override def referencedResources: Seq[Resource] = Seq(file)

  def bulkFile: Option[BulkResource] = {
    if (isBulkResource(file)) {
      Some(asBulkResource(file))
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
  def isBulkResource(resource: Resource): Boolean = {
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
  def asBulkResource(resource: Resource): BulkResource = {
    if (resource.name.endsWith(".zip") && !new File(resource.path).isDirectory) {
      log info s"Zip file Resource found: ${resource.name}"
      BulkResource(new File(resource.path))
    }
    else if (new File(resource.path).isDirectory) {
      log info "Resource Folder found: ${resource.name}"
      throw new NotImplementedError("The bulk resource support does not work for non-zip files for now")    }
    else {
      throw new IllegalArgumentException(resource.path + " is not a bulk resource.")
    }
  }

}
