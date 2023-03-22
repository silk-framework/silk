package org.silkframework.runtime.plugin

import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.runtime.validation.{BadUserInputException, ValidationException}
import org.silkframework.util.{Identifier, StringUtils}
import org.silkframework.workspace.WorkspaceReadTrait

/**
  * Plugin type where each implementation can be used to auto-complete a specific type of parameter, e.g. over workflow tasks
  * or project resources etc.
  *
  * Implementations of this plugin must not have any parameters.
  */
trait PluginParameterAutoCompletionProvider extends AnyPlugin {
  /** Auto-completion based on a text based search query */
  def autoComplete(searchQuery: String,
                   dependOnParameterValues: Seq[ParamValue],
                   workspace: WorkspaceReadTrait)
                  (implicit context: PluginContext): Traversable[AutoCompletionResult]

  /** Returns the label if exists for the given auto-completion value. This is needed if a value should
    * be presented to the user and the actual internal value is e.g. not human-readable.
    *
    * @param projectId The project ID for context.
    * @param value     The value of the parameter.
    * @param dependOnParameterValues The parameter values this parameter auto-completion depends on.
    * */
  def valueToLabel(value: String,
                   dependOnParameterValues: Seq[ParamValue],
                   workspace: WorkspaceReadTrait)
                  (implicit context: PluginContext): Option[String]

  /** Match search terms against string. Returns only true if all search terms match. */
  protected def matchesSearchTerm(lowerCaseSearchTerms: Seq[String],
                                  searchIn: String): Boolean = {
    StringUtils.matchesSearchTerm(lowerCaseSearchTerms, searchIn)
  }

  /** Split text query into multi term search */
  protected def extractSearchTerms(term: String): Array[String] = {
    StringUtils.extractSearchTerms(term)
  }

  /** Auto-completion based on a text query with limit and offset. */
  def autoComplete(searchQuery: String,
                   dependOnParameterValues: Seq[ParamValue],
                   workspace: WorkspaceReadTrait,
                   limit: Int,
                   offset: Int)
                  (implicit context: PluginContext): Traversable[AutoCompletionResult] = {
    autoComplete(searchQuery, dependOnParameterValues, workspace).slice(offset, offset + limit)
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

  /**
    * Retrieves the project from the first dependent parameter.
    * If no dependent parameter is provided, it will fall back to the project in the plugin context.
    * Else it will throw a [[AutoCompletionProjectDependencyException]].
    */
  protected def getProject(dependOnParameterValues: Seq[ParamValue])(implicit context: PluginContext): Identifier = {
    dependOnParameterValues.headOption.map(v => Identifier(v.strValue))
      .orElse(context.projectId)
      .getOrElse(throw AutoCompletionProjectDependencyException("Project not provided"))
  }
}

/** Exception that is thrown if the dependency on a project was not met in the auto-completion provider. */
case class AutoCompletionProjectDependencyException(msg: String) extends RuntimeException(msg)

/**
  * Represents a parameter value.
  *
  * @param strValue The string value.
  * @param paramType The parameter type.
  */
case class ParamValue(strValue: String, paramType: ParameterType[_])

object ParamValue {

  def create(strValue: String, paramName: String, pluginDescription: PluginDescription[_]): ParamValue = {
    ParamValue(strValue, pluginDescription.findParameter(paramName).parameterType)
  }

  def createAll(strValues: Seq[String], paramNames: Seq[String], pluginDescription: PluginDescription[_]): Seq[ParamValue] = {
    if(paramNames.nonEmpty && strValues.size != paramNames.size) {
      throw BadUserInputException("No values for depends-on parameters supplied. Values are expected for " +
        s"following parameters: ${paramNames.mkString(", ")}.")
    }

    for((strValue, paramName) <- strValues zip paramNames) yield {
      create(strValue, paramName, pluginDescription)
    }
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
  override def autoComplete(searchQuery: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                            (implicit context: PluginContext): Traversable[AutoCompletionResult] = Seq.empty

  override def valueToLabel(value: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = None

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
    val plugins = PluginRegistry.pluginDescriptionsById(providerPluginId, Some(Seq(classOf[PluginParameterAutoCompletionProvider])))
        .map { pd =>
          val pluginClass = pd.pluginClass.asInstanceOf[Class[PluginParameterAutoCompletionProvider]]
          get(pluginClass)
        }
    plugins.headOption
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
                            dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                            (implicit context: PluginContext): Traversable[AutoCompletionResult] = {
    autoCompletionProvider.toSeq.flatMap(_.autoComplete(searchQuery, dependOnParameterValues, workspace))
  }

  override def valueToLabel(value: String,
                            dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = {
    autoCompletionProvider.flatMap(_.valueToLabel(value, dependOnParameterValues, workspace))
  }
}