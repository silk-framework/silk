package org.silkframework.dataset.bulk

import java.io.File
import java.util.logging.Logger
import java.util.zip.ZipException

import org.silkframework.dataset.{DataSource, Dataset, ResourceBasedDataset}
import org.silkframework.execution.{InterruptibleTraversable, MappedTraversable}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.resource.zip.ZipResourceLoader
import org.silkframework.runtime.validation.ValidationException

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
    allResources match {
      case Seq(singleResource) =>
        createSource(singleResource)
      case _ =>
        new BulkDataSource(file.name, new MappedTraversable(new InterruptibleTraversable(allResources), createSourceWithName), mergeSchemata)
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
  def allResources: Traversable[Resource] = {
    if (BulkResourceBasedDataset.isBulkResource(file)) {
      BulkResourceBasedDataset.retrieveSubResources(file, internalRegex)
    } else {
      Seq(file)
    }
  }

  /**
    * If this is a bulk file, returns the first sub resource.
    * Otherwise, returns the file itself.
    *
    * @throws ValidationException If this is a bulk file and the bulk file is empty.
    */
  def firstResource: Resource = {
    allResources.headOption.getOrElse(throw new ValidationException(s"Bulk file ${file.name} is empty"))
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

  /**
    * Returns all sub resources inside a bulk resource.
    *
    * Filters the name of the resource via the given filter regex.
    */
  private def retrieveSubResources(resource: Resource, filterRegex: Regex): Traversable[Resource] = {
    if (resource.name.endsWith(".zip") && !new File(resource.path).isDirectory) {
      log fine s"Zip file Resource found: ${resource.name}"
      try {
        new InterruptibleTraversable(
          new Traversable[Resource] {
            override def foreach[U](f: Resource => U): Unit = {
              val zipLoader = ZipResourceLoader(resource, "")
              zipLoader.iterateReadOnceResources(filterRegex) foreach f
            }
          }
        )
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