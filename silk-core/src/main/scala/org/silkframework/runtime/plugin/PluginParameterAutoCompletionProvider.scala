package org.silkframework.runtime.plugin

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.workspace.WorkspaceReadTrait

/**
  * Plugin type where each implementation can be used to auto-complete a specific type of parameter, e.g. over workflow tasks
  * or project resources etc.
  *
  * Implementations of this plugin must not have any parameters.
  */
trait PluginParameterAutoCompletionProvider {
  /** Auto-completion based on a text based search query */
  def autoComplete(searchQuery: String,
                   projectId: String,
                   dependOnParameterValues: Seq[String],
                   workspace: WorkspaceReadTrait)
                  (implicit userContext: UserContext): Traversable[AutoCompletionResult]

  /** Returns the label if exists for the given auto-completion value. This is needed if a value should
    * be presented to the user and the actual internal value is e.g. not human-readable.
    *
    * @param projectId The project ID for context.
    * @param value     The value of the parameter.
    * @param dependOnParameterValues The parameter values this parameter auto-completion depends on.
    * */
  def valueToLabel(projectId: String,
                   value: String,
                   dependOnParameterValues: Seq[String],
                   workspace: WorkspaceReadTrait)
                  (implicit userContext: UserContext): Option[String]

  /** Match search terms against string. Returns only true if all search terms match. */
  protected def matchesSearchTerm(lowerCaseSearchTerms: Seq[String],
                                  searchIn: String): Boolean = {
    if(lowerCaseSearchTerms.isEmpty) {
      true
    } else {
      val lowerCaseText = searchIn.toLowerCase
      lowerCaseSearchTerms forall lowerCaseText.contains
    }
  }

  /** Split text query into multi term search */
  protected def extractSearchTerms(term: String): Array[String] = {
    term.toLowerCase.split("\\s+").filter(_.nonEmpty)
  }

  /** Auto-completion based on a text query with limit and offset. */
  def autoComplete(searchQuery: String,
                   projectId: String,
                   dependOnParameterValues: Seq[String],
                   workspace: WorkspaceReadTrait,
                   limit: Int,
                   offset: Int)
                  (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    autoComplete(searchQuery, projectId, dependOnParameterValues, workspace).slice(offset, offset + limit)
  }

  /** Filters an auto-completion result list by the search query. */
  protected def filterResults(searchQuery: String,
                              results: Traversable[AutoCompletionResult]): Traversable[AutoCompletionResult] = {
    val multiWordSearchQuery = extractSearchTerms(searchQuery)
    results filter { case AutoCompletionResult(value, labelOpt) =>
      val filterBy = labelOpt.getOrElse(value)
      matchesSearchTerm(multiWordSearchQuery, filterBy)
    }
  }

  protected def filterStringResults(searchQuery: String,
                              results: Traversable[String]): Traversable[AutoCompletionResult] = {
    val multiWordSearchQuery = extractSearchTerms(searchQuery)
    results filter (r => matchesSearchTerm(multiWordSearchQuery, r)) map(r => AutoCompletionResult(r, None))
  }
}

/**
  * A single auto-completion result.
  * @param value The value to which the parameter value should be set.
  * @param label An optional label that a human user would see instead. If it is missing the value is shown.
  */
case class AutoCompletionResult(value: String, label: Option[String]) {
  def withNonEmptyLabels: AutoCompletionResult = AutoCompletionResult(value, label.filter(_.trim.nonEmpty))
}

/** Default auto-completion provider. This one always returns empty results. */
case class NopPluginParameterAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {
  override def autoComplete(searchQuery: String, projectId: String, dependOnParameterValues: Seq[String],
                                      workspace: WorkspaceReadTrait)
                                     (implicit userContext: UserContext): Traversable[AutoCompletionResult] = Seq.empty

  override def valueToLabel(projectId: String, value: String, dependOnParameterValues: Seq[String],
                            workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Option[String] = None

}

object PluginParameterAutoCompletionProvider {
  private val providerTrait = classOf[PluginParameterAutoCompletionProvider]
  /** Get an auto-completion plugin by class. */
  def get(providerClass: Class[_ <: PluginParameterAutoCompletionProvider]): PluginParameterAutoCompletionProvider = {
    checkPluginClass(providerClass)
    implicit val prefixes: Prefixes = Prefixes.empty
    implicit val resourceManager: ResourceManager = EmptyResourceManager()
    try {
      providerClass.getConstructor().newInstance()
    } catch {
      case _: NoSuchMethodException =>
        throw new RuntimeException(s"Auto-completion provider class '${providerClass.getCanonicalName}' does not provide an empty constructor.")
    }
  }

  private def checkPluginClass(providerClass: Class[_ <: PluginParameterAutoCompletionProvider]): Unit = {
    assert(classOf[PluginParameterAutoCompletionProvider].isAssignableFrom(providerClass),
      s"Class ${providerClass.getCanonicalName} does not implement ${providerTrait.getCanonicalName}!")
  }

  /** Get an auto-completion plugin by ID. */
  def get(providerPluginId: String): Option[PluginParameterAutoCompletionProvider] = {
    implicit val prefixes: Prefixes = Prefixes.empty
    PluginRegistry.pluginDescriptionById(providerPluginId) map { pd =>
        val pluginClass = pd.pluginClass.asInstanceOf[Class[PluginParameterAutoCompletionProvider]]
      checkPluginClass(pluginClass)
      get(pluginClass)
    }
  }
}

/** This represents a reference to another auto-completion provider by string ID.
  * This unfortunately is necessary sometimes because the auto-completion provider is given as class in the annotation and
  * an implementation could not be written, e.g. because of missing/impossible module dependencies, in the same module as the annotated class.
  * The referenced auto completion provider plugin must be registered in the plugin registry! */
trait ReferencePluginParameterAutoCompletionProvider extends PluginParameterAutoCompletionProvider {
  /** The plugin ID of the auto-completion provider that should actually be used. */
  def pluginParameterAutoCompletionProviderId: String

  private lazy val autoCompletionProvider: Option[PluginParameterAutoCompletionProvider] = {
    PluginParameterAutoCompletionProvider.get(pluginParameterAutoCompletionProviderId)
  }

  override def autoComplete(searchQuery: String,
                                      projectId: String,
                                      dependOnParameterValues: Seq[String],
                                      workspace: WorkspaceReadTrait)
                                     (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    autoCompletionProvider.toSeq.flatMap(_.autoComplete(searchQuery, projectId, dependOnParameterValues, workspace))
  }

  override def valueToLabel(projectId: String,
                            value: String,
                            dependOnParameterValues: Seq[String],
                            workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Option[String] = {
    autoCompletionProvider.flatMap(_.valueToLabel(projectId, value, dependOnParameterValues, workspace))
  }
}