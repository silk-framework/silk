package org.silkframework.rule.execution.local

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, MultiEntityTable}
import org.silkframework.execution.{ExecutionReport, Executor, ExecutorOutput, TaskException}
import org.silkframework.rule._
import org.silkframework.rule.execution.TransformReport
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.util.Uri

import scala.collection.mutable

/** Local executor for link specifications. */
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
    val transformContext = context.asInstanceOf[ActivityContext[TransformReport]]
    transformContext.value() = TransformReport(task)
    input match {
      case mt: MultiEntityTable =>
        val outputTable = mutable.Buffer[LocalEntities]()
        val transformer = new EntityTransformer(task, (mt.asInstanceOf[LocalEntities] +: mt.subTables).to[mutable.Buffer], outputTable, output)
        transformer.transformEntities("root", task.rules, task.outputSchema, transformContext)
        Some(MultiEntityTable(outputTable.head.entities, outputTable.head.entitySchema, task, outputTable.tail, transformContext.value().globalErrors))
      case _ =>
        val outputTable = mutable.Buffer[LocalEntities]()
        val transformer = new EntityTransformer(task, mutable.Buffer(input), outputTable, output)
        transformer.transformEntities("root", task.rules, task.outputSchema, transformContext)
        Some(MultiEntityTable(outputTable.head.entities, outputTable.head.entitySchema, task, outputTable.tail, transformContext.value().globalErrors))
    }
  }

  private class EntityTransformer(task: Task[TransformSpec],
                                  inputTables: mutable.Buffer[LocalEntities],
                                  outputTables: mutable.Buffer[LocalEntities],
                                  output: ExecutorOutput) {

    val requestedOutputSchema: Option[EntitySchema] = output.requestedSchema
    val requestedOutputType: Option[Uri] = requestedOutputSchema.map(_.typeUri)

    def transformEntities(ruleLabel: String,
                          rules: Seq[TransformRule],
                          outputSchema: EntitySchema,
                          context: ActivityContext[TransformReport])
                         (implicit prefixes: Prefixes): Unit = {
      requestedOutputType match {
        case Some(outputType) =>
          val activeOutputSchema = requestedOutputSchema.get
          // If a specific output type is requested only execute rules of that requested type. Only the result table of the matching container rule will be returned.
          val (requestedRuleLabel, requestedRules, inputTable) = findMappingRulesMatchingRequestedOutputSchema(rules, ruleLabel, outputType, inputTables)
          addInputErrorsToTransformReport(inputTable, context)
          val transformedEntities = new TransformedEntities(task, inputTable.entities, requestedRuleLabel, requestedRules, activeOutputSchema,
            isRequestedSchema = true, abortIfErrorsOccur = task.data.abortIfErrorsOccur, context = context)
          outputTables.append(GenericEntityTable(transformedEntities, activeOutputSchema, task))
        case _ =>
          // Else execute the complete mapping
          val inputTable = inputTables.remove(0)
          addInputErrorsToTransformReport(inputTable, context)
          val transformedEntities = new TransformedEntities(task, inputTable.entities, ruleLabel, rules, outputSchema,
            isRequestedSchema = false, abortIfErrorsOccur = task.data.abortIfErrorsOccur, context = context)
          outputTables.append(GenericEntityTable(transformedEntities, outputSchema, task))

          // Recursively transform object mappings and append results to output tables
          for(objectMapping @ ObjectMapping(_, _, _, childRules, _, _) <- rules) {
            val childOutputSchema =
              EntitySchema(
                typeUri = childRules.collectFirst { case tm: TypeMapping => tm.typeUri }.getOrElse(""),
                typedPaths = childRules.flatMap(_.target).map(mt => mt.asTypedPath()).toIndexedSeq
              )

            transformEntities(objectMapping.label(), updateChildRules(childRules, objectMapping), childOutputSchema, context)
          }
      }
    }

    private def addInputErrorsToTransformReport(inputTable: LocalEntities, context: ActivityContext[TransformReport]): Unit = {
      context.value() = context.value().copy(globalErrors = context.value().globalErrors ++ inputTable.globalErrors)
    }

    // Tries to find the container rule that matches the requested output type.
    // Returns the label, rules and input of the matching container rule.
    private def findMappingRulesMatchingRequestedOutputSchema(rules: Seq[TransformRule],
                                                              ruleLabel: String,
                                                              requestedOutputType: Uri,
                                                              inputTables: mutable.Buffer[LocalEntities])
                                                             (implicit prefixes: Prefixes): (String, Seq[TransformRule], LocalEntities) = {
      var matchingTransformRules: Option[Seq[TransformRule]] = None
      var matchingRuleLabel: Option[String] = None
      var inputTable: LocalEntities = inputTables.head
      // Finds first mapping container rule that matches the requested type
      def findRecursive(ruleLabel: String, containerRules: Seq[TransformRule]): Unit = {
        val currentInputTable = inputTables.remove(0)
        if(matchingTransformRules.nonEmpty) {
          // Just return if a rule has already been found
        } else if (containerRules.exists {
          case typeMapping: TypeMapping => typeMapping.typeUri == requestedOutputType
          case _ => false
        }) {
          matchingRuleLabel = Some(ruleLabel)
          matchingTransformRules = Some(containerRules)
          inputTable = currentInputTable
        } else {
          for(objectMapping @ ObjectMapping(_, _, _, childRules, _, _) <- containerRules) {
            findRecursive(objectMapping.label(), updateChildRules(childRules, objectMapping))
          }
        }
      }
      if(requestedOutputType.toString != "") {
        findRecursive(ruleLabel, rules)
      }
      // If nothing else matches or requested type is empty, return root mapping rules and input
      (
        matchingRuleLabel.getOrElse(ruleLabel),
        matchingTransformRules.getOrElse(rules),
        inputTable
      )
    }

    private def updateChildRules(childRules: MappingRules, objectMapping: ObjectMapping): MappingRules = {
      // Add the URI rule of the object mappings, so it generates different default URIs
      childRules.copy(uriRule = childRules.uriRule.orElse(objectMapping.uriRule()))
    }
  }
}
