package org.silkframework.runtime.plugin

import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.{Identifier, StringUtils}
import org.silkframework.workspace.WorkspaceReadTrait

/**
  * Plugin type where each implementation can be used to auto-complete a specific type of parameter, e.g. over workflow tasks
  * or project resources etc.
  *
  * Implementations of this plugin must not have any parameters.
  */
@PluginType()
trait PluginParameterAutoCompletionProvider extends AnyPlugin {
  /** Auto-completion based on a text based search query */
  def autoComplete(searchQuery: String,
                   dependOnParameterValues: Seq[ParamValue],
                   workspace: WorkspaceReadTrait)
                  (implicit context: PluginContext): Iterable[AutoCompletionResult]

  /** Returns the label if exists for the given auto-completion value. This is needed if a value should
    * be presented to the user and the actual internal value is e.g. not human-readable.
    *
    * @param value     The value of the parameter.
    * @param dependOnParameterValues The parameter values this parameter auto-completion depends on.
    * */
  def valueToLabel(value: String,
                   dependOnParameterValues: Seq[ParamValue],
                   workspace: WorkspaceReadTrait)
                  (implicit context: PluginContext): Option[String]

  /** Match search terms against string. Returns only true if all search terms match. */
  protected def matchesSearchTerm(lowerCaseSearchTerms: Iterable[String],
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
                  (implicit context: PluginContext): Iterable[AutoCompletionResult] = {
    autoComplete(searchQuery, dependOnParameterValues, workspace).slice(offset, offset + limit)
  }

  /** Filters an auto-completion result list by the search query. */
  protected def filterResults(searchQuery: String,
                              results: Iterable[AutoCompletionResult]): Iterable[AutoCompletionResult] = {
    val multiWordSearchQuery = extractSearchTerms(searchQuery)
    results filter { case AutoCompletionResult(value, labelOpt) =>
      val filterBy = labelOpt.getOrElse(value)
      matchesSearchTerm(multiWordSearchQuery, filterBy)
    }
  }

  protected def filterStringResults(searchQuery: String,
                              results: Iterable[String]): Iterable[AutoCompletionResult] = {
    val multiWordSearchQuery = extractSearchTerms(searchQuery)
    results filter (r => matchesSearchTerm(multiWordSearchQuery, r)) map(r => AutoCompletionResult(r, None))
  }

  /**
    * Retrieves the project from the first dependent parameter.
    * If no dependent parameter is provided, it will fall back to the project in the plugin context.
    * Else it will throw a [[AutoCompletionProjectDependencyException]].
    */
  protected def getProject(dependOnParameterValues: Seq[ParamValue])(implicit context: PluginContext): Identifier = {
    dependOnParameterValues.headOption.map(v => Identifier(v.value.strValue))
      .orElse(context.projectId)
      .getOrElse(throw AutoCompletionProjectDependencyException("Project not provided"))
  }
}

/** Exception that is thrown if the dependency on a project was not met in the auto-completion provider. */
case class AutoCompletionProjectDependencyException(msg: String) extends RuntimeException(msg)

/**
  * Represents a (simple) parameter value together with its type.
  *
  * @param value The parameter value.
  * @param paramType The parameter type.
  */
case class ParamValue(value: SimpleParameterValue, paramType: ParameterType[_]) {
  def strValue(implicit pluginContext: PluginContext): String = {
    value.strValue
  }
}

object ParamValue {

  def create(value: SimpleParameterValue, paramName: String, pluginDescription: PluginDescription[_]): ParamValue = {
    ParamValue(value, pluginDescription.findParameter(paramName).parameterType)
  }

  def createAll(values: Seq[SimpleParameterValue], paramNames: Seq[String], pluginDescription: PluginDescription[_]): Seq[ParamValue] = {
    if(paramNames.nonEmpty && values.size != paramNames.size) {
      throw BadUserInputException("No values for depends-on parameters supplied. Values are expected for " +
        s"following parameters: ${paramNames.mkString(", ")}.")
    }

    for((value, paramName) <- values zip paramNames) yield {
      create(value, paramName, pluginDescription)
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
                            (implicit context: PluginContext): Iterable[AutoCompletionResult] = Seq.empty

  override def valueToLabel(value: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = None

}

object PluginParameterAutoCompletionProvider {
  private val providerTrait = classOf[PluginParameterAutoCompletionProvider]
  /** Get an auto-completion plugin by class. */
  def get(providerClass: Class[_ <: PluginParameterAutoCompletionProvider]): PluginParameterAutoCompletionProvider = {
    checkPluginClass(providerClass)
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
                            (implicit context: PluginContext): Iterable[AutoCompletionResult] = {
    autoCompletionProvider.toSeq.flatMap(_.autoComplete(searchQuery, dependOnParameterValues, workspace))
  }

  override def valueToLabel(value: String,
                            dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = {
    autoCompletionProvider.flatMap(_.valueToLabel(value, dependOnParameterValues, workspace))
  }
}