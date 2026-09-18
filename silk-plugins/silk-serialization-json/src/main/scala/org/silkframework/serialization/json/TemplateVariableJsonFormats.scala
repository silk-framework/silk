package org.silkframework.serialization.json

import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.runtime.templating.exceptions.TemplateVariablesEvaluationException
import org.silkframework.runtime.templating.{TemplateVariable, TemplateVariables, VariableScope}
import org.silkframework.runtime.validation.BadUserInputException
import play.api.libs.json.{Json, OFormat}

@Schema(description = "A single template variable")
case class TemplateVariableJson(@Schema(
                                    description = "The name of the variable.",
                                    example = "myVar",
                                    requiredMode = RequiredMode.REQUIRED
                                )
                                name: String,
                                @Schema(
                                  description = "The value of the variable. Omitted where sensitive values are masked: for sensitive variables and for variables whose template fails to evaluate.",
                                  example = "example value",
                                  requiredMode = RequiredMode.NOT_REQUIRED
                                )
                                value: Option[String],
                                @Schema(
                                  description = "Template to generate the variable value.",
                                  requiredMode = RequiredMode.NOT_REQUIRED
                                )
                                template: Option[String],
                                @Schema(
                                  description = "Optional description for documentation.",
                                  example = "Example description",
                                  requiredMode = RequiredMode.NOT_REQUIRED
                                )
                                description: Option[String],
                                @Schema(
                                  description = "True, if this is a sensitive variable that should not be exposed to the user. Its value is only available to templates of other sensitive variables and to password parameters.",
                                  example = "false",
                                  requiredMode = RequiredMode.REQUIRED
                                )
                                isSensitive: Boolean,
                                @Schema(
                                  description = "The scope of the variable, e.g., \"project\" or \"execution\".",
                                  example = "project",
                                  requiredMode = RequiredMode.REQUIRED
                                )
                                scope: String) {
  def convert: TemplateVariable = {
    if (value.isEmpty && template.isEmpty) {
      throw new BadUserInputException("Either the variable value or its template has to be defined.")
    }
    TemplateVariable(name, value.getOrElse(""), template, description, isSensitive, VariableScope.parse(scope))
  }
}

object TemplateVariableJson {
  def apply(variable: TemplateVariable): TemplateVariableJson = {
    TemplateVariableJson(variable.name, Some(variable.value), variable.template, variable.description, variable.isSensitive, variable.scope.toString)
  }

  /** Like [[apply]], but omits the value and template of sensitive variables. */
  def masked(variable: TemplateVariable): TemplateVariableJson = {
    if (variable.isSensitive) {
      TemplateVariableJson(variable.name, None, None, variable.description, isSensitive = true, variable.scope.toString)
    } else {
      apply(variable)
    }
  }

  implicit val templateVariableFormat: OFormat[TemplateVariableJson] = Json.format[TemplateVariableJson]
}

@Schema(description = "A list of template variables.")
case class TemplateVariablesJson(@ArraySchema(
                                     schema = new Schema(
                                      description = "List of variables.",
                                      requiredMode = RequiredMode.REQUIRED,
                                      implementation = classOf[TemplateVariableJson]
                                   ))
                                   variables: Seq[TemplateVariableJson],
                                 @ArraySchema(
                                     schema = new Schema(
                                       description = "List of evaluation errors.",
                                       requiredMode = RequiredMode.NOT_REQUIRED,
                                       implementation = classOf[TemplateVariableErrorJson]
                                   ))
                                   errors: Option[Seq[TemplateVariableErrorJson]] = None) {
  def convert: TemplateVariables = {
    TemplateVariables(variables.map(_.convert))
  }
}

object TemplateVariablesJson {
  def apply(variables: TemplateVariables): TemplateVariablesJson = {
    TemplateVariablesJson(variables.variables.map(TemplateVariableJson(_)))
  }

  def apply(variables: TemplateVariables, ex: TemplateVariablesEvaluationException): TemplateVariablesJson = {
    TemplateVariablesJson(variables.variables.map(TemplateVariableJson(_)), Some(ex.issues.map(e => TemplateVariableErrorJson(e.variable.name, e.ex.getMessage))))
  }

  implicit val templateVariablesFormat: OFormat[TemplateVariablesJson] = Json.format[TemplateVariablesJson]
}

@Schema(description = "An error message related to a variable.")
case class TemplateVariableErrorJson(variableName: String, message: String)

object TemplateVariableErrorJson {
  implicit val templateVariableErrorFormat: OFormat[TemplateVariableErrorJson] = Json.format[TemplateVariableErrorJson]
}
