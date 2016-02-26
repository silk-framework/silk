package org.silkframework.execution

import org.silkframework.config.DatasetSelection
import org.silkframework.dataset.{DataSource, EntitySink}
import org.silkframework.entity.EntitySchema
import org.silkframework.rule.TransformRule
import org.silkframework.runtime.activity.{Activity, ActivityContext}

/**
 * Executes a set of transformation rules.
 */
class ExecuteTransform(input: DataSource,
                       selection: DatasetSelection,
                       rules: Seq[TransformRule],
                       outputs: Seq[EntitySink] = Seq.empty) extends Activity[Unit] {

  require(rules.count(_.target.isEmpty) <= 1, "Only one rule with empty target property (subject rule) allowed.")

  private val subjectRule = rules.find(_.target.isEmpty)

  private val propertyRules = rules.filter(_.target.isDefined)

  @volatile
  private var isCanceled: Boolean = false

  def run(context: ActivityContext[Unit]): Unit = {
    isCanceled = false
    // Retrieve entities
    val entitySchema =
      EntitySchema(
        typeUri = selection.typeUri,
        paths = rules.flatMap(_.paths).distinct.toIndexedSeq,
        filter = selection.restriction
      )
    val entities = input.retrieve(entitySchema)

    try {
      // Open outputs
      val properties = propertyRules.map(_.target.get.uri)
      for(output <- outputs) output.open(properties)

      // Transform all entities and write to outputs
      var count = 0
      for(entity <- entities) {
        val uri = subjectRule.flatMap(_(entity).headOption).getOrElse(entity.uri)
        val values = propertyRules.map(_(entity))
        for (output <- outputs)
          output.writeEntity(uri, values)
        if(isCanceled)
          return
        count += 1
        if(count % 1000 == 0)
          context.status.updateMessage(s"Executing ($count Entities)")
      }
      context.status.update(s"$count entities written to ${outputs.size} outputs", 1.0)
    } finally {
      // Close outputs
      for (output <- outputs) output.close()
    }
  }

  override def cancelExecution(): Unit = {
    isCanceled = true
  }
}