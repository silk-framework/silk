package org.silkframework.dataset

import org.silkframework.config.TaskSpec
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.plugin.{AnyPlugin, PluginFactory}

/**
 * Manages the access to a specific dataset.
 */
trait Dataset extends TaskSpec with AnyPlugin with DatasetAccess {

  /** Datasets don't define input schemata, because any data can be written to them. */
  override lazy val inputSchemataOpt: Option[Seq[EntitySchema]] = None

  /** Datasets don't have a static EntitySchema. It is defined by the following task. */
  override lazy val outputSchemaOpt: Option[EntitySchema] = None
}

trait DatasetPluginAutoConfigurable[T <: Dataset] {
  /**
   * returns an auto-configured version of this plugin
   */
  def autoConfigured: T
}

/**
 * Creates new dataset olugin instances.
 */
object Dataset extends PluginFactory[Dataset]