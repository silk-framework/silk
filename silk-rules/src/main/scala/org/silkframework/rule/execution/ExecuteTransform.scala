package org.silkframework.rule.execution

import org.silkframework.dataset.{DataSource, EntitySink, TypedProperty}
import org.silkframework.entity.{Entity, EntitySchema, Path, TypedPath}
import org.silkframework.execution.ExecutionReport
import org.silkframework.rule.{HierarchicalMapping, TransformSpec, TypeMapping}
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

    try {
      for (output <- outputs) {
        output.open(transform.outputSchema.typedPaths.map(_.property.get))
      }

      val entities = input.retrieve(transform.inputSchema)

      val transformedEntities = new TransformedEntities(entities, transform.rules, transform.outputSchema, context.asInstanceOf[ActivityContext[ExecutionReport]])
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

    val output = outputs.head

    for(rule: HierarchicalMapping <- transform.rules) {
      val childPaths = rule.childRules.flatMap(_.target).map(p => TypedProperty(p.propertyUri.toString, p.valueType))
      output.open(childPaths)

      val entities = input.retrieve(transform.inputSchema.child(rule.relativePath))

      val childOutputSchema =
        EntitySchema(
          typeUri = rule.childRules.collect { case tm: TypeMapping => tm.typeUri }.headOption.getOrElse(""),
          typedPaths = rule.childRules.flatMap(_.target).map(mt => TypedPath(Path(mt.propertyUri), mt.valueType)).toIndexedSeq
        )

      val transformedEntities = new TransformedEntities(entities, transform.rules, childOutputSchema, context.asInstanceOf[ActivityContext[ExecutionReport]])

      for (entity <- transformedEntities) {
        for (output <- outputs) {
          output.writeEntity(entity.uri, entity.values)
        }
        if (isCanceled) {
          return
        }
      }

    }

  }

  override def cancelExecution(): Unit = {
    isCanceled = true
  }
}