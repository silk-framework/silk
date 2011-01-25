package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.LinkingModule
import modules.source.SourceModule

trait Project
{
  /**
   * Retrieves the project configuration.
   */
  def config : ProjectConfig

  /**
   * Updates the project configuration.
   */
  def config_=(config : ProjectConfig) : Unit

  /**
   * The source module which encapsulates all data sources.
   */
  def sourceModule : SourceModule

  /**
   * The linking module which encapsulates all linking tasks.
   */
  def linkingModule : LinkingModule
}
