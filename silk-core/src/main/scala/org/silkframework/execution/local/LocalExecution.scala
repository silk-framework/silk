package org.silkframework.execution.local

import org.silkframework.dataset.Dataset
import org.silkframework.dataset.DirtyTrackingFileDataSink.{log, updatedFiles}
import org.silkframework.execution.ExecutionType
import org.silkframework.execution.local.LocalExecution.LocalInternalDataset
import org.silkframework.plugins.dataset.{InternalDataset, InternalDatasetTrait}
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.Identifier

import java.util.concurrent.ConcurrentHashMap
import java.util.logging.{Level, Logger}
import scala.collection.mutable
import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala

/**
  * Tasks are executed on a local machine.
  *
  * @param useLocalInternalDatasets If set to true the LocalExecution object will maintain an own map of internal
  *                                 datasets, instead of using the global ones.
  */
case class LocalExecution(useLocalInternalDatasets: Boolean,
                          replaceDataSources: Map[String, Dataset] = Map.empty,
                          replaceSinks: Map[String, Dataset] = Map.empty,
                          workflowId: Option[Identifier] = None) extends ExecutionType {

  type DataType = LocalEntities

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  private val internalDatasets: mutable.Map[Option[String], InternalDatasetTrait] = mutable.Map.empty
  private val shutdownHooks = new ConcurrentHashMap[Identifier, () => Unit]()
  private val logger = Logger.getLogger(LocalExecution.getClass.getName)
  // Tracks updated files that were overwritten with means other than the sink of a file based dataset
  private val updatedFiles = new ConcurrentHashMap[String, String]()

  /**
    * Fetches all resources from a collection that have been updated and clears from the update list.
    */
  def fetchAndClearUpdatedFiles(resources: Iterable[Resource]): Set[String] = this.synchronized {
    var foundEntries =  Set[String]()
    for(resource <- resources) {
      if(updatedFiles.remove(resource.name) != null) {
        foundEntries += resource.name
      }
    }
    foundEntries
  }

  def addUpdatedFile(resourceName: String): Unit = this.synchronized {
    log.fine(s"File $resourceName has been updated!")
    updatedFiles.put(resourceName, resourceName)
  }

  def createInternalDataset(internalDatasetId: Option[String]): Dataset = {
    if (useLocalInternalDatasets) {
      internalDatasets.synchronized {
        internalDatasets.getOrElseUpdate(
          internalDatasetId,
          new LocalInternalDataset
        )
      }
    } else {
      InternalDataset.byGraph(Option(generateGraphUri(internalDatasetId)))
    }
  }

  private def generateGraphUri(internalDatasetId: Option[String]): String = {
    val uriOpt = internalDatasetId map { id =>
      InternalDataset.internalDatasetGraphPrefix + id
    }
    uriOpt.orNull // graphUri must be set to null for the default
  }

  def addShutdownHook(hook: () => Unit): Identifier ={
    val id = Identifier.random
    this.shutdownHooks.put(id, hook)
    id
  }

  def cancleShutdownHook(id: Identifier): Unit ={
    this.shutdownHooks.remove(id)
  }

  def executeShutdownHooks(): Unit ={
    for((id, hook) <- this.shutdownHooks.asScala){
      try{
        hook()
      }
      catch{
        case ex: Throwable => logger.log(Level.FINE, "Exception while executing an Execution shutdown hook.", ex)
      }
    }
    this.shutdownHooks.clear()
  }
}

object LocalExecution {

  private lazy val instance = LocalExecution(false)

  def apply(): LocalExecution = {
    instance
  }

  class LocalInternalDataset extends InternalDatasetTrait {
    override protected def internalDatasetPluginImpl: Dataset = InternalDataset.createInternalDataset()
  }

}
