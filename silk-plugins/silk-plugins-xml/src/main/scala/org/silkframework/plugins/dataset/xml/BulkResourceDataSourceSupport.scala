package org.silkframework.plugins.dataset.xml

import java.io.File
import java.util.logging.Logger

import org.silkframework.dataset._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{BulkResource, Resource}

trait BulkResourceDataSourceSupport {

  private final val log: Logger = Logger.getLogger(this.getClass.getSimpleName)

  /**
    * Create a BulkResource based DataSource object. Returns the original source if given and no BulkResource is detected.
    *
    * @param dataset Dataset used in creation of BulkDataSource
    * @param originalSource Source that get returned if no zip/folder is found
    * @return A DataSource object
    */
  def checkAndReplaceDatasource(dataset: Dataset, originalSource: Option[DataSource] = None): DataSource = {
    implicit val userContext = UserContext.INTERNAL_USER
    val source: Option[DataSource] = dataset match {
      case ds: ResourceBasedDataset => {
        if (isBulkResource(ds.file)) {
          Some(BulkResourceDataSource(asBulkResource(ds.file), dataset, isStreaming = false))
        }
        else {
          log severe "Warning no BulResource was detected, if no default source is provided errors may occur. "
          originalSource
        }
      }
      case _ => {
        throw new RuntimeException("A dataset with bulk resor ce support must be resource based.")
      }
    }
    source.getOrElse(throw new RuntimeException("A bulkresource could not be instantiated for: " + dataset.pluginSpec.id))
  }

//  /**
//    * To be implemnted by data soruce. Provides a BulkResource object based DataSource or
//    * not if the file is no BulkResource. Left to the trait imlpemeting class, since it would get to
//    * comple to handle all cases i the trait function.
//    *
//    * @return
//    */
//  def handleBulkResource(): DataSource

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
      BulkResource(new File(resource.path))
    }
    else {
      throw new IllegalArgumentException(resource.path + " is not a bulk resource.")
    }
  }

}
