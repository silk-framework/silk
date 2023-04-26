package org.silkframework.runtime.templating

import org.silkframework.entity.Entity
import org.silkframework.runtime.plugin.AnyPlugin
import org.silkframework.runtime.plugin.annotations.PluginType

import java.io.Writer

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
  def variables: Option[Seq[String]] = None

  /**
    * Evaluates this template using provided values.
    *
    * @throws TemplateEvaluationException If the evaluation failed.
    */
  def evaluate(values: Map[String, AnyRef], writer: Writer)

  /**
    * Evaluates this template using a provided entity.
    *
    * @throws TemplateEvaluationException If the evaluation failed.
    */
  def evaluate(entity: Entity, writer: Writer): Unit = {
    evaluate(entityToMap(entity), writer)
  }

  protected def entityToMap(entity: Entity): Map[String, AnyRef] = {
    val keyValues =
      for((path, value) <- entity.schema.typedPaths zip entity.values if value.nonEmpty) yield {
        path.normalizedSerialization -> RuleValues.fromValues(value)
      }
    keyValues.toMap
  }

}
