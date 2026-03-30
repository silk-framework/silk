package org.silkframework.runtime.templating

import org.silkframework.entity.Entity
import org.silkframework.runtime.plugin.AnyPlugin
import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.resource.{Resource, ResourceTooLargeException}
import org.silkframework.runtime.templating.exceptions.TemplateEvaluationException

import java.io.Writer
import scala.collection.mutable
import scala.jdk.CollectionConverters.{BufferHasAsJava, MapHasAsJava}

/**
  * A template engine that supports compiling template strings.
  */
@PluginType()
trait TemplateEngine extends AnyPlugin {

  def compile(templateString: String): CompiledTemplate
}

/**
  * A compile template that can be evaluated.
  */
trait CompiledTemplate {

  /**
    * Holds all unbound variables in the template.
    * Returns None, if this functionality is not supported.
    */
  def variables: Option[Seq[TemplateVariableName]] = None

  /**
    * Returns all method usages on a given variable in the template.
    * Each usage contains the method name and its string parameter value.
    * Only methods with a single string constant parameter are returned.
    * Returns an empty sequence by default if not supported by the template engine.
    */
  def methodUsages(variableName: String): Seq[TemplateMethodUsage] = Seq.empty

  /**
   * Evaluates this template using a map of variable values.
   */
  def evaluate(values: Map[String, AnyRef], writer: Writer): Unit

  /**
    * Evaluates this template using provided values.
    *
    * @throws TemplateEvaluationException If the evaluation failed.
    */
  def evaluate(values: Seq[TemplateVariableValue], writer: Writer, evaluationConfig: EvaluationConfig = EvaluationConfig()): Unit

  /**
   * Converts template values to a Java Map
   */
  protected def convertValues(value: Seq[TemplateVariableValue]): Map[String, AnyRef] = {
    value.groupBy(_.scope).flatMap { case (scope, values) =>
      if (scope.isEmpty) {
        for (value <- values) yield {
          (value.name, IterableTemplateValues.fromValues(value.values))
        }
      } else {
        val nestedValues =
          for (value <- values) yield {
            (value.name, IterableTemplateValues.fromValues(value.values))
          }
        Seq((scope, nestedValues.toMap.asJava))
      }
    }
  }
}

/** Config to fine-tune evaluation parameters for the template engine.
  *
  * @param ignoreUnboundVariables If an unbound variable is found then instead of throwing an error the variable evaluates
  *                               to the variable name itself.
  */
case class EvaluationConfig(ignoreUnboundVariables: Boolean = false)

/**
  * Represents a method invocation on a template variable with a single string parameter.
  *
  * @param methodName     The name of the invoked method.
  * @param parameterValue The string constant passed as parameter.
  */
case class TemplateMethodUsage(methodName: String, parameterValue: String)