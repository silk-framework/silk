package org.silkframework.rule.execution

import org.silkframework.dataset.DataSource
import org.silkframework.entity.EntitySchema
import org.silkframework.rule.TransformRule
import org.silkframework.rule.evaluation.{DetailedEntity, DetailedEvaluator}
import org.silkframework.runtime.plugin.PluginContext

import java.util.logging.Logger

/**
 * Evaluates a transformation rule.
 * In contrast to ExecuteTransform, this task generates a detailed output that for each entity
 * containing all intermediate values of the rule evaluation.
 */
class EvaluateTransform(source: DataSource,
                        entitySchema: EntitySchema,
                        rules: Seq[TransformRule],
                        maxEntities: Int = 100) {

  private val log = Logger.getLogger(getClass.getName)

  @volatile
  private var cachedValues = Seq[DetailedEntity]()

  def execute()(implicit context: PluginContext): Seq[DetailedEntity] = {
    // Retrieve entities
    source.retrieve(entitySchema, Some(maxEntities)).entities.use { entities =>
      // Read all entities
      for (entity <- entities) {
        // Transform entity
        val transformedEntity = DetailedEvaluator(rules, entity)
        // Write transformed entity to cache
        cachedValues = cachedValues :+ transformedEntity
        if (cachedValues.size >= maxEntities) return cachedValues
      }
    }

    cachedValues
  }
}

object EvaluateTransform {
  def empty = new EvaluateTransform(null, null, null)
}