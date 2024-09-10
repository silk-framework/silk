package org.silkframework.rule.execution.local

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, MultiEntityTable}
import org.silkframework.execution.{ExecutionReport, Executor, ExecutorOutput, TaskException}
import org.silkframework.rule.TransformSpec.RuleSchemata
import org.silkframework.rule._
import org.silkframework.rule.execution.{TransformReport, TransformReportBuilder}
import org.silkframework.rule.TaskContext
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.Uri

import scala.collection.mutable

/** Local executor for link specifications. */
class LocalTransformSpecExecutor extends Executor[TransformSpec, LocalExecution] {

  override def execute(task: Task[TransformSpec],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    val input = inputs.headOption.getOrElse {
      throw TaskException("No input given to transform specification executor " + task.id + "!")
    }
    val transformContext = context.asInstanceOf[ActivityContext[TransformReport]]
    transformContext.value() = TransformReport(task)
    val flatInputs = flattenInputs(input).toIndexedSeq
    val outputTables = mutable.Buffer[LocalEntities]()
    val ruleSchemata = task.data.ruleSchemataWithoutEmptyObjectRules
    val report = new TransformReportBuilder(task, transformContext)
    implicit val prefixes: Prefixes = pluginContext.prefixes
    implicit val taskContext: TaskContext = TaskContext(Seq(input.task), pluginContext)

    for ((ruleSchema, index) <- ruleSchemata.zipWithIndex) {
      val input = flatInputs(index)
      val outputTable = transformEntities(task, ruleSchema, input, output.requestedSchema, report)
      outputTables.append(outputTable)
      context.status.updateProgress((index + 1.0) / ruleSchemata.size)
    }

    Some(MultiEntityTable(outputTables.head.entities, outputTables.head.entitySchema, task, outputTables.tail.toSeq, transformContext.value().globalErrors))
  }

  def transformEntities(task: Task[TransformSpec],
                        schemata: RuleSchemata,
                        input: LocalEntities,
                        requestedOutputSchema: Option[EntitySchema],
                        report: TransformReportBuilder)
                       (implicit prefixes: Prefixes, taskContext: TaskContext): LocalEntities = {

    val rule = schemata.transformRule
    val ruleLabel = rule.label()
    val requestedOutputType: Option[Uri] = requestedOutputSchema.map(_.typeUri)

    requestedOutputType match {
      case Some(outputType) =>
        val rules = rule.rules
        val activeOutputSchema = requestedOutputSchema.get
        // If a specific output type is requested only execute rules of that requested type. Only the result table of the matching container rule will be returned.
        val inputTables = flattenInputs(input).toBuffer
        val (requestedRuleLabel, requestedRules, inputTable) = findMappingRulesMatchingRequestedOutputSchema(rules, ruleLabel, outputType, inputTables)
        addInputErrorsToTransformReport(inputTable, report)
        val transformedEntities = new TransformedEntities(task, inputTable.entities, requestedRuleLabel,
          rule.withChildren(requestedRules).withContext(taskContext), activeOutputSchema,
          isRequestedSchema = true, abortIfErrorsOccur = task.data.abortIfErrorsOccur, report).iterator
        GenericEntityTable(transformedEntities, activeOutputSchema, task)
      case _ =>
        // Else execute the complete mapping
        addInputErrorsToTransformReport(input, report)
        val transformedEntities = new TransformedEntities(task, input.entities, ruleLabel, rule.withContext(taskContext), schemata.outputSchema,
          isRequestedSchema = false, abortIfErrorsOccur = task.data.abortIfErrorsOccur, report).iterator
        GenericEntityTable(transformedEntities, schemata.outputSchema, task)
    }
  }

  private def addInputErrorsToTransformReport(inputTable: LocalEntities, report: TransformReportBuilder): Unit = {
    report.addGlobalErrors(inputTable.globalErrors)
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

  private def flattenInputs(input: LocalEntities): Seq[LocalEntities] = {
    input match {
      case mt: MultiEntityTable =>
        mt.allTables
      case _ =>
        Seq(input)
    }
  }
}
