package de.fuberlin.wiwiss.silk.execution

import de.fuberlin.wiwiss.silk.runtime.task.{ValueTask, Task}
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.entity.{Link, EntityDescription}
import de.fuberlin.wiwiss.silk.config.Dataset
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.linkagerule.evaluation.{TransformedValue, DetailedEvaluator, Value}

/**
 * Evaluates a transformation rule.
 * In contrast to ExecuteTransform, this task generates a detailed output that for each entity
 * contains all intermediate values of the rule evaluation.
 */
class EvaluateTransform(source: Source,
                        dataset: Dataset,
                        rule: TransformRule,
                        outputs: Traversable[Output] = Traversable.empty) extends Task[Any] {

  private val log = Logger.getLogger(getClass.getName)

  private val cacheSize = 100

  @volatile
  private var cachedValues = Seq[Value]()

  lazy val cache = { execute(); cachedValues }

  def execute(): Any = {
    // Retrieve entities
    val entityDesc =
      new EntityDescription(
        variable = dataset.variable,
        restrictions = dataset.restriction,
        paths = rule.paths.toIndexedSeq
      )
    val entities = source.retrieve(entityDesc)

    // Open outputs
    for(output <- outputs) output.open()

    // Transform all entities and write to outputs
    for(entity <- entities;
        detailedValue <- DetailedEvaluator(rule, entity)) {

      for(output <- outputs; value <- detailedValue.values)
        output.write(new Link(entity.uri, value), rule.targetProperty)

      if(cachedValues.size < cacheSize) {
        cachedValues = cachedValues :+ detailedValue
      }
    }

    // Close outputs
    for(output <- outputs) output.close()
  }
}

object EvaluateTransform {
  def empty = new EvaluateTransform(null, null, null, null)
}