package org.silkframework.workspace

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.{GlobalTemplateVariables, VariableScope, TemplateVariables, TemplateVariablesManager}
import org.silkframework.workspace.changes.{ChangeJournal, VariableChanges}

/**
 * Manages project template variables.
 *
 * @param serializer The serializer to read and write template variables.
 * @param loadingUser The user context for loading the variables initially.
 * @param changeJournal The journal that records each variable change.
 */
class ProjectTemplateVariablesManager(serializer: TemplateVariablesSerializer, loadingUser: UserContext,
                                      changeJournal: ChangeJournal) extends TemplateVariablesManager {

  private def projectScope = VariableScope.project

  @volatile
  private var variables: TemplateVariables = serializer.readVariables()(loadingUser)

  /**
    * The available variable scopes.
    */
  def scopes: Set[VariableScope] = GlobalTemplateVariables.scopes + projectScope

  /**
    * Returns the global variables as the parent scope.
    */
  override def parentVariables: TemplateVariables = GlobalTemplateVariables.all

  /**
    * All managed variables must be in the project scope.
    */
  override def variableScope: VariableScope = projectScope

  /**
    * Retrieves all template variables.
    */
  override def all: TemplateVariables = {
    variables
  }

  /**
    * Updates all template variables. Records each added, changed or removed variable in the change journal.
    */
  override def put(variables: TemplateVariables)(implicit user: UserContext): Unit = synchronized {
    validateScope(variables)
    val changes = VariableChanges.diff(this.variables, variables)
    serializer.putVariables(variables)
    this.variables = variables
    changes.foreach(change => changeJournal.record(change))
  }
}
