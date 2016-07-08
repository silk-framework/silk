package org.silkframework.execution

import org.silkframework.config.DatasetSelection
import org.silkframework.dataset.{DataSource, EntitySink}
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.ExecuteTransformResult.RuleError
import org.silkframework.rule.TransformRule
import org.silkframework.runtime.activity.{Activity, ActivityContext}

/**
  * Executes a set of transformation rules.
  */
class ExecuteTransform(input: DataSource,
                       selection: DatasetSelection,
                       rules: Seq[TransformRule],
                       outputs: Seq[EntitySink] = Seq.empty,
                       errorOutputs: Seq[EntitySink]) extends Activity[ExecuteTransformResult] {

  require(rules.count(_.target.isEmpty) <= 1, "Only one rule with empty target property (subject rule) allowed.")

  private val subjectRule = rules.find(_.target.isEmpty)

  private val propertyRules = rules.filter(_.target.isDefined)

  @volatile
  private var isCanceled: Boolean = false

  lazy val entitySchema: EntitySchema = {
    EntitySchema(
      typeUri = selection.typeUri,
      paths = rules.flatMap(_.paths).distinct.toIndexedSeq,
      filter = selection.restriction
    )
  }

  override val initialValue = Some(ExecuteTransformResult())

  def run(context: ActivityContext[ExecuteTransformResult]): Unit = {
    isCanceled = false
    // Retrieve entities
    val entities = input.retrieve(entitySchema)

    // Transform statistics
    var entityCounter = 0L
    var entityErrorCounter = 0L
    var errorResults = ExecuteTransformResult.initial(propertyRules)
    try {
      // Open outputs
      val properties = propertyRules.map(_.target.get.uri)
      for (output <- outputs) output.open(properties)
      val inputProperties = entitySchema.paths.map( p =>
        p.propertyUri.map(_.uri).getOrElse(p.toString)).toIndexedSeq
      for (errorOutput <- errorOutputs) errorOutput.open(inputProperties)

      // Transform all entities and write to outputs
      var count = 0
      for (entity <- entities) {
        entityCounter += 1
        val uri = subjectRule.flatMap(_ (entity).headOption).getOrElse(entity.uri)
        var success = true
        val values = propertyRules.map { r =>
          try {
            r(entity)
          } catch {
            case ex: Exception =>
              success = false
              val values = r.paths.map(entity.evaluate)
              errorResults = errorResults.withError(r.name, RuleError(uri, values, ex))
              Seq()
          }
        }
        if(success) {
          for (output <- outputs) {
            output.writeEntity(uri, values)
          }
        } else {
          entityErrorCounter += 1
          for (errorOutput <- errorOutputs) {
            errorOutput.writeEntity(uri, entity.values)
          }
        }
        if (isCanceled)
          return
        count += 1
        if (count % 1000 == 0) {
          context.value.update(errorResults.copy(entityCounter, entityErrorCounter, errorResults.ruleResults))
          context.status.updateMessage(s"Executing ($count Entities)")
        }
      }
      context.status.update(s"$count entities written to ${outputs.size} outputs", 1.0)
    } finally {
      // Set final value
      context.value.update(errorResults.copy(entityCounter, entityErrorCounter, errorResults.ruleResults))
      // Close outputs
      for (output <- outputs) output.close()
      for (errorOutput <- errorOutputs) errorOutput.close()
    }
  }

  override def cancelExecution(): Unit = {
    isCanceled = true
  }
}