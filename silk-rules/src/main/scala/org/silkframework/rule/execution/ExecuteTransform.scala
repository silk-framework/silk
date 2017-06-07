package org.silkframework.rule.execution

import org.silkframework.dataset.{DataSource, EntitySink, TypedProperty}
import org.silkframework.entity._
import org.silkframework.execution.ExecutionReport
import org.silkframework.rule._
import org.silkframework.rule.execution.local.TransformedEntities
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.runtime.validation.ValidationException

/**
  * Executes a set of transformation rules.
  */
class ExecuteTransform(input: DataSource, transform: TransformSpec, outputs: Seq[EntitySink]) extends Activity[TransformReport] {

  require(transform.rules.count(_.target.isEmpty) <= 1, "Only one rule with empty target property (subject rule) allowed.")

  private val subjectRule = transform.rules.find(_.target.isEmpty)

  private val propertyRules = transform.rules.filter(_.target.isDefined)

  @volatile
  private var isCanceled: Boolean = false

  override val initialValue = Some(TransformReport())

  def run(context: ActivityContext[TransformReport]): Unit = {
    isCanceled = false

    transformEntities(transform.inputSchema, transform.rules, transform.outputSchema, context)
  }

  private def transformEntities(inputSchema: EntitySchema, rules: Seq[TransformRule], outputSchema: EntitySchema,
                                context: ActivityContext[TransformReport]): Unit = {
    try {
      for (output <- outputs) {
        output.open(outputSchema.typedPaths.map(_.property.get))
      }

      val entities = input.retrieve(inputSchema)

      val transformedEntities = new TransformedEntities(entities, rules, outputSchema, context.asInstanceOf[ActivityContext[ExecutionReport]])
      for (entity <- transformedEntities) {
        for (output <- outputs) {
          output.writeEntity(entity.uri, entity.values)
        }
        if (isCanceled) {
          return
        }
      }

    } finally {
      for (output <- outputs) {
        output.close()
      }
    }

    for(ObjectMapping(_, relativePath, _, childRules, _) <- rules) {
      val childInputSchema =
        EntitySchema(
          typeUri = inputSchema.typeUri,
          typedPaths = childRules.flatMap(_.paths).map(p => TypedPath(p, StringValueType)).distinct.toIndexedSeq,
          subPath = relativePath
        )
      val childOutputSchema =
        EntitySchema(
          typeUri = childRules.collect { case tm: TypeMapping => tm.typeUri }.headOption.getOrElse(""),
          typedPaths = childRules.flatMap(_.target).map(mt => TypedPath(Path(mt.propertyUri), mt.valueType)).toIndexedSeq
        )

      transformEntities(childInputSchema, childRules, childOutputSchema, context)
    }
  }

  override def cancelExecution(): Unit = {
    isCanceled = true
  }
}