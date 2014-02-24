package de.fuberlin.wiwiss.silk.execution

import de.fuberlin.wiwiss.silk.runtime.task.{ValueTask, Task}
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.entity.{Link, EntityDescription}
import de.fuberlin.wiwiss.silk.config.Dataset
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.linkagerule.evaluation.{DetailedEntity, DetailedEvaluator, TransformedValue, Value}

/**
 * Evaluates a transformation rule.
 * In contrast to ExecuteTransform, this task generates a detailed output that for each entity
 * contains all intermediate values of the rule evaluation.
 */
class EvaluateTransform(source: Source,
                        dataset: Dataset,
                        rules: Seq[TransformRule],
                        maxEntities: Int = 100) extends Task[Seq[DetailedEntity]] {

  private val log = Logger.getLogger(getClass.getName)

  @volatile
  private var cachedValues = Seq[DetailedEntity]()

  lazy val cache = { execute(); cachedValues }

  def execute(): Seq[DetailedEntity] = {
    // Retrieve entities
    val entityDesc =
      new EntityDescription(
        variable = dataset.variable,
        restrictions = dataset.restriction,
        paths = rules.flatMap(_.paths).toIndexedSeq
      )
    val entities = source.retrieve(entityDesc)

    // Read all entities
    for(entity <- entities) {
      // Transform entity
      val transformedEntity = DetailedEvaluator(rules, entity)
      // Write transformed entity to cache
      cachedValues = cachedValues :+ transformedEntity
      if(cachedValues.size >= maxEntities) return cachedValues
    }

    cachedValues
  }
}

object EvaluateTransform {
  def empty = new EvaluateTransform(null, null, null)
}