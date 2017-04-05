package org.silkframework.rule.execution

import org.silkframework.dataset.{DataSource, EntitySink}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.ExecutionReport
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.local.TransformedEntities
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.runtime.validation.ValidationException

/**
  * Executes a set of transformation rules.
  */
class ExecuteTransform(entities: Traversable[Entity], transform: TransformSpec, outputs: Seq[EntitySink]) extends Activity[TransformReport] {

  def this(dataSource: DataSource, transformSpec: TransformSpec, outputs: Seq[EntitySink]) = {
    this(dataSource.retrieve(transformSpec.inputSchema), transformSpec, outputs)
  }

  require(transform.rules.count(_.target.isEmpty) <= 1, "Only one rule with empty target property (subject rule) allowed.")

  private val subjectRule = transform.rules.find(_.target.isEmpty)

  private val propertyRules = transform.rules.filter(_.target.isDefined)

  @volatile
  private var isCanceled: Boolean = false

  lazy val entitySchema: EntitySchema = {
    EntitySchema(
      typeUri = transform.selection.typeUri,
      typedPaths = transform.rules.flatMap(_.paths).distinct.map(_.asStringTypedPath).toIndexedSeq,
      filter = transform.selection.restriction
    )
  }

  override val initialValue = Some(TransformReport())

  def run(context: ActivityContext[TransformReport]): Unit = {
    isCanceled = false

//    if(!valueType.validate(value)) {
//      throw new ValidationException(s"Value $value is invalid according to target type ${valueType.label}")
//    }

    try {
      for (output <- outputs) {
        output.open(transform.outputSchema.typedPaths.map(_.property.get))
      }

      val transformedEntities = new TransformedEntities(entities, transform, transform.outputSchema, context.asInstanceOf[ActivityContext[ExecutionReport]])
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
  }

  override def cancelExecution(): Unit = {
    isCanceled = true
  }
}