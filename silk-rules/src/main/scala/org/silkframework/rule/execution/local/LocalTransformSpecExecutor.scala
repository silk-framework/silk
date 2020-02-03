package org.silkframework.rule.execution.local

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, MultiEntityTable}
import org.silkframework.execution.{ExecutionReport, Executor, ExecutorOutput, TaskException}
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
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit userContext: UserContext, prefixes: Prefixes): Option[LocalEntities] = {
    val input = inputs.headOption.getOrElse {
      throw TaskException("No input given to transform specification executor " + task.id + "!")
    }
    val outputSchema = output.requestedSchema.getOrElse(task.outputSchema)
    val transformContext = context.asInstanceOf[ActivityContext[TransformReport]]
    transformContext.value() = TransformReport(task.taskLabel())
    input match {
      case mt: MultiEntityTable =>
        val outputTable = mutable.Buffer[LocalEntities]()
        val transformer = new EntityTransformer(task, (mt.asInstanceOf[LocalEntities] +: mt.subTables).to[mutable.Buffer], outputTable, output)
        transformer.transformEntities(task.rules, outputSchema, transformContext)
        Some(MultiEntityTable(outputTable.head.entities, outputTable.head.entitySchema, task, outputTable.tail, transformContext.value().globalErrors))
      case _ =>
        val outputTable = mutable.Buffer[LocalEntities]()
        val transformer = new EntityTransformer(task, mutable.Buffer(input), outputTable, output)
        transformer.transformEntities(task.rules, outputSchema, transformContext)
        Some(MultiEntityTable(outputTable.head.entities, outputTable.head.entitySchema, task, outputTable.tail, transformContext.value().globalErrors))
    }
  }

  private class EntityTransformer(task: Task[TransformSpec],
                                  inputTables: mutable.Buffer[LocalEntities],
                                  outputTables: mutable.Buffer[LocalEntities],
                                  output: ExecutorOutput) {

    def transformEntities(rules: Seq[TransformRule],
                          outputSchema: EntitySchema,
                          context: ActivityContext[TransformReport]): Unit = {

      val inputTable = inputTables.remove(0)

      // Add input errors to transformation report
      context.value() = context.value().copy(globalErrors = context.value().globalErrors ++ inputTable.globalErrors)

      val transformedEntities = new TransformedEntities(task.taskLabel(), inputTable.entities, rules, outputSchema,
        isRequestedSchema = output.requestedSchema.isDefined, context = context)
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
