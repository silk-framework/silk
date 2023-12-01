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
   * Evaluates this template using a map of variable values.
   */
  def evaluate(values: Map[String, AnyRef], writer: Writer): Unit

  /**
    * Evaluates this template using provided values.
    *
    * @throws TemplateEvaluationException If the evaluation failed.
    */
  def evaluate(values: Seq[TemplateVariableValue], writer: Writer): Unit

  /**
    * Evaluates this template using a provided entity.
    *
    * @throws TemplateEvaluationException If the evaluation failed.
    */
  def evaluate(entity: Entity, writer: Writer): Unit = {
    evaluate(entityToMap(entity), writer)
  }

  /**
   * Evaluates this template using an iterator over entities.
   *
   * @throws TemplateEvaluationException If the evaluation failed.
   */
  def evaluate(entities: Iterator[Entity], writer: Writer): Unit = {
    evaluate(Map("entities" -> loadEntitiesIntoMemory(entities)), writer)
  }

  /**
   * Converts an entity to a sequence of template variables.
   */
  protected def entityToMap(entity: Entity): Seq[TemplateVariableValue] = {
    for((path, value) <- entity.schema.typedPaths zip entity.values if value.nonEmpty) yield {
       new TemplateVariableValue(path.normalizedSerialization, "", value)
    }
  }

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

  /**
   * Loads an iterator of entities into a Java map.
   */
  private def loadEntitiesIntoMemory(entities: Iterator[Entity]): java.util.List[java.util.Map[String, AnyRef]] = {
    val buffer = mutable.Buffer[java.util.Map[String, AnyRef]]()
    val maxCount = Resource.maxInMemorySize()
    var byteCount = 0
    for (entity <- entities) {
      byteCount += entity.uri.uri.length * 2
      for (values <- entity.values; value <- values) {
        byteCount += value.length * 2
      }
      if (byteCount > maxCount) {
        throw new ResourceTooLargeException("Tried to load an entities into memory that are larger than the configured maximum " +
          s"(size: $byteCount, maximum size: $maxCount}). " +
          s"Configure '${Resource.maxInMemorySizeParameterName}' in order to increase this limit.")
      }
      buffer.append(convertValues(entityToMap(entity)).asJava)
    }
    buffer.asJava
  }

}
