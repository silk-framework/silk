package org.silkframework.execution

import java.util.concurrent.atomic.AtomicLong

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
                       outputs: Seq[EntitySink] = Seq.empty) extends Activity[ExecuteTransformResult] {

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

  override val initialValue = Some(ExecuteTransformResult(0l, 0l, Map()))

  def run(context: ActivityContext[ExecuteTransformResult]): Unit = {
    isCanceled = false
    // Retrieve entities
    val entities = input.retrieve(entitySchema)

    // Transform statistics
    var entityCounter = 0l
    var entityErrorCounter = 0l
    val ruleErrorCounter = propertyRules.map(pr => (pr.name.toString, new AtomicLong(0))).toMap
    try {
      // Open outputs
      val properties = propertyRules.map(_.target.get.uri)
      for (output <- outputs) output.open(properties)

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
            case e: Exception =>
              success = false
              ruleErrorCounter(r.name.toString).incrementAndGet()
              Seq()
          }
        }
        if(success) {
          for (output <- outputs)
            output.writeEntity(uri, values)
        } else {
          entityErrorCounter += 1
        }
        if (isCanceled)
          return
        count += 1
        if (count % 1000 == 0) {
          context.value.update(executeTransformResult(entityCounter, entityErrorCounter, ruleErrorCounter))
          context.status.updateMessage(s"Executing ($count Entities)")
        }
      }
      context.status.update(s"$count entities written to ${outputs.size} outputs", 1.0)
    } finally {
      // Close outputs
      context.value.update(executeTransformResult(entityCounter, entityErrorCounter, ruleErrorCounter))
      for (output <- outputs) output.close()
    }
  }

  private def executeTransformResult(entityCounter: Long,
                                     entityErrorCounter: Long,
                                     ruleErrorCounter: Map[String, AtomicLong]): ExecuteTransformResult = {
    val ruleErrorCountLong = ruleErrorCounter.map { case (name, counter) =>
      (name, counter.get())
    }
    ExecuteTransformResult(entityCounter, entityErrorCounter, ruleErrorCountLong)
  }

  override def cancelExecution(): Unit = {
    isCanceled = true
  }
}