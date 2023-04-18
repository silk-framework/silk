package org.silkframework.workspace

import org.silkframework.runtime.templating.{GlobalTemplateVariables, GlobalTemplateVariablesConfig, TemplateVariable, TemplateVariables, TemplateVariablesManager}
import org.silkframework.util.Identifier

class ProjectTemplateVariablesManager(project: Identifier, provider: WorkspaceProvider) extends TemplateVariablesManager {

  private val projectScope = "project"

  /**
    * The available variable scopes.
    */
  def scopes: Set[String] = GlobalTemplateVariables.scopes + projectScope

  /**
    * Retrieves all template variables.
    */
  override def all: TemplateVariables = GlobalTemplateVariables.all

  /**
    * Add or update a template variable.
    */
  override def put(variable: TemplateVariable): Unit = ???

  /**
    * Remove an existing template variable.
    */
  override def remove(name: String): Unit = ???
}
