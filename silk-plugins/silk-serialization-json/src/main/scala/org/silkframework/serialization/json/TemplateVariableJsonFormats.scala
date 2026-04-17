package org.silkframework.serialization.json

import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.runtime.templating.exceptions.TemplateVariablesEvaluationException
import org.silkframework.runtime.templating.{TemplateVariable, TemplateVariables}
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
                                  description = "The value of the variable.",
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
                                  description = "True, if this is a sensitive variable that should not be exposed to the user.",
                                  example = "false",
                                  requiredMode = RequiredMode.REQUIRED
                                )
                                isSensitive: Boolean,
                                @Schema(
                                  description = "The scope of the variable as a sequence of strings forming a prefix path, e.g. [\"project\"] or [\"project\", \"metaData\"].",
                                  requiredMode = RequiredMode.REQUIRED
                                )
                                scope: Seq[String]) {
  def convert: TemplateVariable = {
    if (value.isEmpty && template.isEmpty) {
      throw new BadUserInputException("Either the variable value or its template has to be defined.")
    }
    TemplateVariable(name, value.getOrElse(""), template, description, isSensitive, scope)
  }
}

object TemplateVariableJson {
  def apply(variable: TemplateVariable): TemplateVariableJson = {
    TemplateVariableJson(variable.name, Some(variable.value), variable.template, variable.description, variable.isSensitive, variable.scope)
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
