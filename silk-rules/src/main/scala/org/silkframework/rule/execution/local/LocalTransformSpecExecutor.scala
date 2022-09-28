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
    transformContext.value() = TransformReport(task)
    input match {
      case mt: MultiEntityTable =>
        val outputTable = mutable.Buffer[LocalEntities]()
        val transformer = new EntityTransformer(task, (mt.asInstanceOf[LocalEntities] +: mt.subTables).to[mutable.Buffer], outputTable, output)
        transformer.transformEntities("root", task.rules, outputSchema, transformContext)
        Some(MultiEntityTable(outputTable.head.entities, outputTable.head.entitySchema, task, outputTable.tail, transformContext.value().globalErrors))
      case _ =>
        val outputTable = mutable.Buffer[LocalEntities]()
        val transformer = new EntityTransformer(task, mutable.Buffer(input), outputTable, output)
        transformer.transformEntities("root", task.rules, outputSchema, transformContext)
        Some(MultiEntityTable(outputTable.head.entities, outputTable.head.entitySchema, task, outputTable.tail, transformContext.value().globalErrors))
    }
  }

  private class EntityTransformer(task: Task[TransformSpec],
                                  inputTables: mutable.Buffer[LocalEntities],
                                  outputTables: mutable.Buffer[LocalEntities],
                                  output: ExecutorOutput) {

    def transformEntities(ruleLabel: String,
                          rules: Seq[TransformRule],
                          outputSchema: EntitySchema,
                          context: ActivityContext[TransformReport])
                         (implicit prefixes: Prefixes): Unit = {

      val inputTable = inputTables.remove(0)

      // Add input errors to transformation report
      context.value() = context.value().copy(globalErrors = context.value().globalErrors ++ inputTable.globalErrors)

      val transformedEntities = new TransformedEntities(task, inputTable.entities, ruleLabel, rules, outputSchema,
        isRequestedSchema = output.requestedSchema.isDefined, abortIfErrorsOccur = task.data.abortIfErrorsOccur, context = context)
      outputTables.append(GenericEntityTable(transformedEntities, outputSchema, task))

      for(objectMapping @ ObjectMapping(_, _, _, childRules, _, _) <- rules
          if childRules.typeRules.nonEmpty || childRules.propertyRules.nonEmpty) {
        val childOutputSchema =
          EntitySchema(
            typeUri = childRules.collectFirst { case tm: TypeMapping => tm.typeUri }.getOrElse(""),
            typedPaths = childRules.flatMap(_.target).map(mt => mt.asTypedPath()).toIndexedSeq
          )

        val updatedChildRules = childRules.copy(uriRule = childRules.uriRule.orElse(objectMapping.uriRule()))

        transformEntities(objectMapping.label(), updatedChildRules, childOutputSchema, context)
      }
    }
  }
}
