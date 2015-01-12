package de.fuberlin.wiwiss.silk.dataset

import de.fuberlin.wiwiss.silk.runtime.plugin.{PluginFactory, AnyPlugin}

/**
 * Manages the access to a specific dataset.
 */
trait DatasetPlugin extends AnyPlugin {

  /**
   * Returns a data source for reading entities from the data set.
   */
  def source: DataSource

  /**
   * Returns a data sink for writing data to the data set.
   */
  def sink: DataSink

}


/**
 * Creates new dataset olugin instances.
 */
object DatasetPlugin extends PluginFactory[DatasetPlugin]