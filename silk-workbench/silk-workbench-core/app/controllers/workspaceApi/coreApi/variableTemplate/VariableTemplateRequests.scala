package controllers.workspaceApi.coreApi.variableTemplate

import controllers.autoCompletion._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.exceptions.{SensitiveVariableReferenceException, TemplateEvaluationException, TemplateSyntaxException, UnboundVariablesException}
import org.silkframework.runtime.templating.{EvaluationConfig, GlobalTemplateVariables, GlobalTemplateVariablesConfig, TemplateVariableName, TemplateVariables, VariableScope}
import org.silkframework.util.StringUtils
import org.silkframework.workspace.WorkspaceFactory
import play.api.libs.json.{Format, Json}

import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

trait VariableTemplateRequest {

  def project: Option[String]

  def variableName: Option[String]

  def task: Option[String]

  /**
    * Collects all variables given the optional project, task, and variable name.
    * Unless sensitive variables are included, sensitive parents are withheld and sensitive siblings are only
    * available to a sensitive variable, as when the variable is saved (see TemplateVariables.referenceable).
    */
  def collectVariables(ignoreVariableName: Boolean = false, includeSensitiveVariables: Boolean = false)(implicit user: UserContext): TemplateVariables = {
    project match {
      case Some(projectName) =>
        val manager = WorkspaceFactory().workspace.project(projectName).variablesManager(task)
        // Parent scopes + the target scope's variables up to the current variable (later ones cannot be referenced)
        var scopeVariables = manager.all.variables
        for (name <- variableName if !ignoreVariableName) {
          scopeVariables = scopeVariables.takeWhile(_.name != name)
        }
        val parentVariables = if (includeSensitiveVariables) manager.parentVariables.variables else manager.parentVariables.withoutSensitiveVariables().variables
        if (!includeSensitiveVariables && !validatesSensitiveVariable(manager.all)) {
          scopeVariables = scopeVariables.filterNot(_.isSensitive)
        }
        TemplateVariables(parentVariables ++ scopeVariables)
      case None =>
        if (includeSensitiveVariables) GlobalTemplateVariables.all else GlobalTemplateVariables.all.withoutSensitiveVariables()
    }
  }

  /** True if the variable being edited exists in the given scope and is sensitive, so that it may reference sensitive siblings. */
  protected def validatesSensitiveVariable(scopeVariables: TemplateVariables): Boolean = {
    variableName.exists(name => scopeVariables.map.get(name).exists(_.isSensitive))
  }

}

case class ValidateVariableTemplateRequest(templateString: String,
                                           project: Option[String] = None,
                                           variableName: Option[String] = None,
                                           task: Option[String] = None,
                                           includeSensitiveVariables: Option[Boolean] = None,
                                           ignoreUnboundVariables: Option[Boolean] = None) extends VariableTemplateRequest {
  private val evaluationConfig: EvaluationConfig = EvaluationConfig(ignoreUnboundVariables = ignoreUnboundVariables.getOrElse(false))

  def execute()(implicit user: UserContext): VariableTemplateValidationResponse = {
    val variables = collectVariables(includeSensitiveVariables = includeSensitiveVariables.getOrElse(false))
    for(missingVariable <- missingKnownScopedVariable(variables)) {
      return invalidVariable(missingVariable)
    }
    val isWithheldSensitive = withheldSensitiveVariable()
    try {
      val evaluatedTemplate = variables.resolveTemplateValue(templateString, evaluationConfig)
      valid(Some(evaluatedTemplate))
    } catch {
      case ex: UnboundVariablesException if ex.missingVars.exists(isWithheldSensitive) =>
        // Same rule and message as saving the variable, see TemplateVariables.resolveTemplate
        val (withheld, otherMissing) = ex.missingVars.partition(isWithheldSensitive)
        invalid(SensitiveVariableReferenceException.message(withheld, otherMissing))
      case ex: UnboundVariablesException if variableName.isDefined && ex.missingVars.size == 1 =>
        // Check if the variable is unbound because it is defined after the current one
        Try(collectVariables(ignoreVariableName = true).resolveTemplateValue(templateString, evaluationConfig)) match {
          case _: Success[_] =>
            invalid(s"'${ex.missingVars.head}' cannot be used because it's defined after '${variableName.get}'.")
          case _: Failure[_] =>
            invalid(ex.getMessage)
        }
      case ex: UnboundVariablesException if !evaluationConfig.ignoreUnboundVariables && ex.missingVars.size == 1 =>
        invalidVariable(ex.missingVars.head.scopedName)
      case ex: TemplateSyntaxException =>
        // Syntax errors do not depend on the variable values, so they are also reported in lenient mode
        invalid(ex.getMessage)
      case _: TemplateEvaluationException if evaluationConfig.ignoreUnboundVariables =>
        // Lenient mode evaluates on placeholder values, so evaluation errors are expected (e.g. iterating over a placeholder).
        // The template counts as valid, but no preview is provided.
        valid(None)
      case NonFatal(ex) =>
        invalid(ex.getMessage)
    }
  }

  /**
    * Tells whether a missing variable is a sensitive variable of the validated scope, withheld from the template of a
    * non-sensitive variable. Only when a non-sensitive variable is validated: a sensitive one sees its sensitive
    * siblings, and a parameter template follows the password-parameter rule instead and reports the withheld variable
    * as not defined, as in lenient mode.
    */
  private def withheldSensitiveVariable()(implicit user: UserContext): TemplateVariableName => Boolean = {
    project match {
      case Some(projectName) if variableName.isDefined && !includeSensitiveVariables.getOrElse(false) =>
        val scopeVariables = WorkspaceFactory().workspace.project(projectName).variablesManager(task).all
        if (validatesSensitiveVariable(scopeVariables)) _ => false else scopeVariables.isSensitiveMember
      case _ =>
        _ => false
    }
  }

  /**
    * In lenient mode, references to scopes whose variables are fully known in this request context
    * (global always, project and execution if given) are still checked for existence.
    * Method calls on variables (e.g. 'project.myVar.trim()') are unaffected, since the collected
    * reference is the variable itself. Property access (e.g. 'project.myVar.length') is reported:
    * it never resolves on variable values at execution time either.
    * Returns the first missing variable, if any.
    */
  private def missingKnownScopedVariable(providedVariables: TemplateVariables): Option[String] = {
    if(!evaluationConfig.ignoreUnboundVariables) {
      None
    } else {
      val checkedRoots = (Seq(VariableScope.global) ++ project.map(_ => VariableScope.project) ++ task.map(_ => VariableScope.execution)).map(_.toString)
      val templateVariables =
        try {
          GlobalTemplateVariablesConfig.templateEngine().compile(templateString).variables.getOrElse(Seq.empty)
        } catch {
          case NonFatal(_) =>
            Seq.empty // Errors are reported by the evaluation
        }
      val providedNames = providedVariables.variables.map(_.scopedName).toSet
      templateVariables.find { variable =>
        variable.scope.path.headOption.exists(checkedRoots.contains) && !providedNames.contains(variable.scopedName)
      }.map(_.scopedName)
    }
  }

  private def valid(evaluatedTemplate: Option[String]): VariableTemplateValidationResponse = {
    VariableTemplateValidationResponse(valid = true, parseError = None, evaluatedTemplate = evaluatedTemplate)
  }

  /** Validation error for a missing variable that underlines its first occurrence instead of the whole template. */
  private def invalidVariable(scopedName: String): VariableTemplateValidationResponse = {
    val start = templateString.indexOf(scopedName)
    val error =
      if(start != -1) VariableTemplateValidationError(s"'$scopedName' is not defined.", start, start + scopedName.length)
      else VariableTemplateValidationError(s"'$scopedName' is not defined.", 0, templateString.length)
    VariableTemplateValidationResponse(valid = false, parseError = Some(error), evaluatedTemplate = None)
  }

  private def invalid(errorMessage: String): VariableTemplateValidationResponse = {
    VariableTemplateValidationResponse(
      valid = false,
      parseError = Some(VariableTemplateValidationError(message = errorMessage, start = 0, end = templateString.length)),
      evaluatedTemplate = None
    )
  }

}

object ValidateVariableTemplateRequest {
  implicit val validateVariableTemplateRequestFormat: Format[ValidateVariableTemplateRequest] = Json.format[ValidateVariableTemplateRequest]
}

/**
  * Response for a validation request that can be understood by the auto-suggest UI component.
  *
  * @param valid             If the input string is valid or not.
  * @param parseError        If not valid, this contains the parse error details.
  * @param evaluatedTemplate If valid then this will containt the evaluated template.
  */
case class VariableTemplateValidationResponse(valid: Boolean,
                                              parseError: Option[VariableTemplateValidationError],
                                              evaluatedTemplate: Option[String])

/** A validation error for a single line input. */
case class VariableTemplateValidationError(message: String, start: Int, end: Int)

object VariableTemplateValidationResponse {
  implicit val autoSuggestValidationErrorFormat: Format[VariableTemplateValidationError] = Json.format[VariableTemplateValidationError]
  implicit val variableTemplateValidationResponseFormat : Format[VariableTemplateValidationResponse] = Json.format[VariableTemplateValidationResponse]
}

/** Variable template auto-completion request. */
case class AutoCompleteVariableTemplateRequest(inputString: String,
                                               cursorPosition: Int,
                                               maxSuggestions: Option[Int],
                                               project: Option[String] = None,
                                               variableName: Option[String] = None,
                                               task: Option[String] = None,
                                               includeSensitiveVariables: Option[Boolean] = None) extends AutoSuggestAutoCompletionRequest with VariableTemplateRequest {
  def execute()(implicit user: UserContext): AutoSuggestAutoCompletionResponse = {
    AutoCompleteVariableTemplateRequest.suggestions(this, collectVariables(includeSensitiveVariables = includeSensitiveVariables.getOrElse(false)).variableNames)
  }
}

object AutoCompleteVariableTemplateRequest {
  implicit val autoCompleteVariableTemplateRequestFormat: Format[AutoCompleteVariableTemplateRequest] = Json.format[AutoCompleteVariableTemplateRequest]

  def suggestions(request: AutoCompleteVariableTemplateRequest, variables: Seq[String]): AutoSuggestAutoCompletionResponse = {
    val searchQueryOpt = extractSearchString(request)
    val filteredVariables = searchQueryOpt match {
      case Some((extractedQuery, _)) =>
        filterVariables(variables, extractedQuery)
      case None =>
        Seq.empty
    }
    AutoSuggestAutoCompletionResponse(
      request.inputString,
      request.cursorPosition,
      replacementResults = Seq(ReplacementResults(
        replacementInterval = searchQueryOpt.map(s => ReplacementInterval(s._2, s._1.length)).getOrElse(ReplacementInterval(request.cursorPosition, 0)),
        extractedQuery = searchQueryOpt.map(_._1).getOrElse(""),
        replacements = filteredVariables.map(v => CompletionBase(v))
      ))
    )
  }

  /** If a query has been found for a variable completion, the extracted query and the start index of the string is returned. */
  private def extractSearchString(request: AutoCompleteVariableTemplateRequest): Option[(String, Int)] = {
    val idx = request.pathUntilCursor.reverse.indexOf("{{")
    if (idx != -1) {
      val queryUntilCursor = request.pathUntilCursor.reverse.take(idx).reverse.stripLeading()
      val queryAfterCursor = request.inputString.drop(request.cursorPosition).takeWhile(c => {
        c.isLetterOrDigit || c == '.'
      })
      if(queryUntilCursor.contains("}")) {
        // Do not complete when not inside of variable expression
        None
      } else {
        Some(queryUntilCursor + queryAfterCursor, math.min(request.cursorPosition, request.inputString.length) - queryUntilCursor.length)
      }
    } else {
      // No search string, do not return results
      None
    }
  }

  private def filterVariables(variables: Seq[String],
                              searchString: String): Seq[String] = {
    val searchWords = StringUtils.extractSearchTerms(searchString)
    if (searchWords.isEmpty) {
      variables
    } else {
      variables.filter(v => StringUtils.matchesSearchTerm(searchWords, v))
    }
  }
}