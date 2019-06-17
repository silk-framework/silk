package org.silkframework.rule.execution.local

import org.silkframework.config.Task
import org.silkframework.entity.{EntitySchema, TypedPath}
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, MultiEntityTable}
import org.silkframework.execution.{ExecutionReport, Executor, TaskException}
import org.silkframework.rule._
import org.silkframework.rule.execution.TransformReport
import org.silkframework.runtime.activity.{ActivityContext, UserContext}

import scala.collection.mutable

/**
  * Created on 7/20/16.
  */
class LocalTransformSpecExecutor extends Executor[TransformSpec, LocalExecution] {

  override def execute(task: Task[TransformSpec],
                       inputs: Seq[LocalEntities],
                       outputSchema: Option[EntitySchema],
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit userContext: UserContext): Option[LocalEntities] = {
    val input = inputs.headOption.getOrElse {
      throw TaskException("No input given to transform specification executor " + task.id + "!")
    }

    input match {
      case mt: MultiEntityTable =>
        val output = mutable.Buffer[LocalEntities]()
        val transformer = new EntityTransformer(task, (mt.asInstanceOf[LocalEntities] +: mt.subTables).to[mutable.Buffer], output)
        transformer.transformEntities(task.rules, task.outputSchema, context)
        Some(MultiEntityTable(output.head.entities, output.head.entitySchema, task, output.tail))
      case _ =>
        val output = mutable.Buffer[LocalEntities]()
        val transformer = new EntityTransformer(task, mutable.Buffer(input), output)
        transformer.transformEntities(task.rules, task.outputSchema, context)
        Some(MultiEntityTable(output.head.entities, output.head.entitySchema, task, output.tail))
    }
  }

  private class EntityTransformer(task: Task[TransformSpec], inputTables: mutable.Buffer[LocalEntities], outputTables: mutable.Buffer[LocalEntities]) {

    def transformEntities(rules: Seq[TransformRule], outputSchema: EntitySchema,
                                  context: ActivityContext[ExecutionReport]): Unit = {

      val entities = inputTables.remove(0).entities

      val transformedEntities = new TransformedEntities(task.taskLabel(), entities, rules, outputSchema, context.asInstanceOf[ActivityContext[TransformReport]])
      outputTables.append(GenericEntityTable(transformedEntities, outputSchema, task))

      for(objectMapping @ ObjectMapping(_, relativePath, _, childRules, _, _) <- rules) {
        val childOutputSchema =
          EntitySchema(
            typeUri = childRules.collectFirst { case tm: TypeMapping => tm.typeUri }.getOrElse(""),
            typedPaths = childRules.flatMap(_.target).map(mt => mt.asTypedPath()).toIndexedSeq
          )

        val updatedChildRules = childRules.copy(uriRule = childRules.uriRule.orElse(objectMapping.uriRule()))

        transformEntities(updatedChildRules, childOutputSchema, context)
      }
    }
  }
}
