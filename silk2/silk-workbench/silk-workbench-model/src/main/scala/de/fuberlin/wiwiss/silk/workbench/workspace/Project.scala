package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.LinkingModule
import modules.output.OutputModule
import modules.source.SourceModule
import de.fuberlin.wiwiss.silk.util.Identifier

trait Project
{
  /**
   * The name of this project
   */
  val name : Identifier

  /**
   * Retrieves the project configuration.
   */
  def config : ProjectConfig

  /**
   * Updates the project configuration.
   */
  def config_=(config : ProjectConfig)

  /**
   * The source module which encapsulates all data sources.
   */
  def sourceModule : SourceModule

  /**
   * The linking module which encapsulates all linking tasks.
   */
  def linkingModule : LinkingModule

  /**
   * The output module which encapsulates all outputs.
   */
  def outputModule : OutputModule
}
