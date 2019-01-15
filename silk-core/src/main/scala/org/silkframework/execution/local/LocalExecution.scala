package org.silkframework.execution.local

import java.util.concurrent.ConcurrentHashMap
import java.util.logging.{Level, Logger}

import org.silkframework.dataset.Dataset
import org.silkframework.execution.ExecutionType
import org.silkframework.plugins.dataset.{InternalDataset, InternalDatasetTrait}
import org.silkframework.util.Identifier

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Tasks are executed on a local machine.
  *
  * @param useLocalInternalDatasets If set to true the LocalExecution object will maintain an own map of internal
  *                                 datasets, instead of using the global ones.
  */
case class LocalExecution(useLocalInternalDatasets: Boolean) extends ExecutionType {

  type DataType = LocalEntities

  private val internalDatasets: mutable.Map[Option[String], InternalDatasetTrait] = mutable.Map.empty
  private val shutdownHooks = new ConcurrentHashMap[Identifier, () => Unit]()
  private val logger = Logger.getLogger(LocalExecution.getClass.getName)

  def createInternalDataset(internalDatasetId: Option[String]): Dataset = {
    if (useLocalInternalDatasets) {
      internalDatasets.synchronized {
        internalDatasets.getOrElseUpdate(
          internalDatasetId,
          new InternalDatasetTrait {
            override protected def internalDatasetPluginImpl: Dataset = InternalDataset.createInternalDataset()
          }
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

  def addShutdownHook(id: Identifier, hook: () => Unit): Identifier ={
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
