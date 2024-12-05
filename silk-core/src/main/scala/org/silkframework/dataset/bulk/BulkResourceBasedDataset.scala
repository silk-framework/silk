package org.silkframework.dataset.bulk

import org.silkframework.dataset._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.resource.zip.ZipInputStreamResourceIterator
import org.silkframework.runtime.resource.{Resource, WritableResource}
import org.silkframework.runtime.validation.ValidationException

import java.io.File
import java.util.logging.Logger
import java.util.zip.ZipException
import scala.util.control.NonFatal
import scala.util.matching.Regex

/**
  * A resource based data source for which the resource could also be a zip archive.
  * If the resource is a zip archive, all files in the zip archive are read.
  * If the resource is not a zip archive, the resource is read directly.
  *
  * @see ResourceBasedDataset
  */
trait BulkResourceBasedDataset extends ResourceBasedDataset { this: Dataset =>

  /**
    * Determines if the schemata of the underlying data sources should be merged.
    * If true, the types and paths of the underlying data sources are merged.
    * If false, the types and paths of the first data source are used.
    */
  def mergeSchemata: Boolean

  /** A file regex specifying the files in the zip file that are part of the dataset, e.g. '.*\.csv$', '.*\.xml$'. */
  def zipFileRegex: String = ".*" // by default all files in the archive

  /**
   * Resource that will write to the underlying file.
   * If the underlying file is a zip, it will write to a single file within this zip.
   */
  def bulkWritableResource: WritableResource = {
    writableResource match {
      case Some(outputResource) =>
        if (BulkResourceBasedDataset.isBulkResource(file)) {
          ZipWritableResource(outputResource)
        } else {
          outputResource
        }
      case _ =>
        throw new ValidationException(s"Cannot write to $this.")
    }
  }

  private val internalRegex = zipFileRegex.trim.r

  /**
    * Creates a data source for a particular resource inside the bulk file.
    */
  def createSource(resource: Resource): DataSource

  private def createSourceWithName(resource: Resource): DataSourceWithName = {
    DataSourceWithName(resource.name, createSource(resource))
  }

  /**
    * Returns a data source for reading entities from the data set.
    */
  override final def source(implicit userContext: UserContext): DataSource = {
    if(BulkResourceBasedDataset.isBulkResource(file)) {
      new BulkDataSource(file.name, () => retrieveResources().map(createSourceWithName), mergeSchemata)
    } else {
      createSource(file)
    }
  }

  /**
    * Checks if the resource is a bulk archive.
    */
  def isBulkResource: Boolean = BulkResourceBasedDataset.isBulkResource(file)

  /**
    * Returns all resources this dataset is based on.
    * If the dataset is based on a bulk file, this returns all sub resources.
    * Otherwise, returns the file itself.
    */
  def retrieveResources(): CloseableIterator[Resource] = {
    if (BulkResourceBasedDataset.isBulkResource(file)) {
      BulkResourceBasedDataset.retrieveSubResources(file, internalRegex)
    } else {
      CloseableIterator(Seq(file))
    }
  }

  /**
    * If this is a bulk file, returns the first sub resource.
    * Otherwise, returns the file itself.
    *
    * @throws ValidationException If this is a bulk file and the bulk file is empty.
    */
  def useFirstResource[T](f: Resource => T): T = {
    retrieveResources().use { iterator =>
      if(iterator.hasNext) {
        f(iterator.next())
      } else {
        throw new ValidationException(s"Bulk file ${file.name} is empty")
      }
    }
  }
}

object BulkResourceBasedDataset {

  private final val log: Logger = Logger.getLogger(this.getClass.getSimpleName)

  /**
    * Returns true if the given resource is a BulkResource and false otherwise.
    * A BulkResource is detected if the file belonging to the given resource ends with .zip or is a
    * directory.
    *
    * @param resource WritableResource to check
    * @return true if an archive or folder
    */
  private def isBulkResource(resource: Resource): Boolean = {
    resource.name.endsWith(".zip") && !new File(resource.path).isDirectory
  }

  def isZip(resource: Resource): Boolean = resource.name.endsWith(".zip")

  /**
    * Returns all sub resources inside a bulk resource.
    *
    * Filters the name of the resource via the given filter regex.
    */
  private def retrieveSubResources(resource: Resource, filterRegex: Regex): CloseableIterator[Resource] = {
    if (resource.name.endsWith(".zip") && !new File(resource.path).isDirectory) {
      log fine s"Zip file Resource found: ${resource.name}"
      try {
        val zipLoader = ZipInputStreamResourceIterator(resource, "")
        zipLoader.iterateReadOnceResources(filterRegex)
      } catch {
        case NonFatal(t) =>
          log warning s"Exception for zip resource ${resource.path}: " + t.getMessage
          throw new ZipException(t.getMessage)
      }
    } else if (new File(resource.path).isDirectory) {
      log fine s"Resource Folder found: ${resource.name}"
      throw new NotImplementedError("The bulk resource support does not work for non-zip files for now")
    } else {
      throw new IllegalArgumentException(resource.path + " is not a bulk resource.")
    }
  }
}

/** A data source with a named resource */
protected case class DataSourceWithName(resourceName: String, source: DataSource)