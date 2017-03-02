package org.silkframework.rule.execution

import java.util.logging.Logger

import org.silkframework.dataset.DataSource
import org.silkframework.entity.EntitySchema
import org.silkframework.rule.{DatasetSelection, TransformRule}
import org.silkframework.rule.evaluation.{DetailedEntity, DetailedEvaluator}

/**
 * Evaluates a transformation rule.
 * In contrast to ExecuteTransform, this task generates a detailed output that for each entity
 * containing all intermediate values of the rule evaluation.
 */
class EvaluateTransform(source: DataSource,
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
      EntitySchema(
        typeUri = dataSelection.typeUri,
        typedPaths = rules.flatMap(_.paths).map(_.asStringTypedPath).toIndexedSeq,
        filter = dataSelection.restriction
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