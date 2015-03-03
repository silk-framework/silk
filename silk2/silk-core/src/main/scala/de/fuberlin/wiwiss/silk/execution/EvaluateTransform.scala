package de.fuberlin.wiwiss.silk.execution

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.config.DatasetSelection
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.entity.EntityDescription
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.linkagerule.evaluation.{DetailedEntity, DetailedEvaluator}
import de.fuberlin.wiwiss.silk.runtime.oldtask.Task

/**
 * Evaluates a transformation rule.
 * In contrast to ExecuteTransform, this task generates a detailed output that for each entity
 * containing all intermediate values of the rule evaluation.
 */
class EvaluateTransform(source: Dataset,
                        dataSelection: DatasetSelection,
                        rules: Seq[TransformRule],
                        maxEntities: Int = 100) {

  private val log = Logger.getLogger(getClass.getName)

  @volatile
  private var cachedValues = Seq[DetailedEntity]()

  lazy val cache = { execute(); cachedValues }

  def execute(): Seq[DetailedEntity] = {
    // Retrieve entities
    val entityDesc =
      new EntityDescription(
        variable = dataSelection.variable,
        restrictions = dataSelection.restriction,
        paths = rules.flatMap(_.paths).toIndexedSeq
      )
    val entities = source.source.retrieve(entityDesc)

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