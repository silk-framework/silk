package org.silkframework.rule.evaluation

import org.silkframework.config.PlainTask
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.rule.input.{PathInput, RuleBlockBindingExecution, RuleBlockExecution}
import org.silkframework.rule.{RuleBlockInputExample, RuleBlockModel, RuleBlockSpec, TaskContext}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.{Identifier, Uri}

/** An input example together with the evaluated operator tree of the rule block it was fed into. */
case class EvaluatedRuleBlockExample(example: RuleBlockInputExample, value: Value)

/**
  * Evaluates a rule block on its own: its ports are fed from example values instead of from a calling
  * rule, so a block can be evaluated without a transform or linking task that uses it.
  */
object RuleBlockEvaluation {

  /** Evaluates the block for each of its stored input examples. Empty if the block has no operator tree. */
  def evaluateInputExamples(taskId: Identifier, ruleBlockModel: RuleBlockModel)
                           (implicit pluginContext: PluginContext): Seq[EvaluatedRuleBlockExample] = {
    val evaluate = evaluator(taskId, ruleBlockModel)
    ruleBlockModel.inputExamples.flatMap(example => evaluate(example).map(EvaluatedRuleBlockExample(example, _)))
  }

  /** Evaluates the block for a single set of port values. Empty if the block has no operator tree. */
  def evaluate(taskId: Identifier, ruleBlockModel: RuleBlockModel, inputs: Map[Identifier, Seq[String]])
              (implicit pluginContext: PluginContext): Option[Value] = {
    val evaluateExample = evaluator(taskId, ruleBlockModel)
    evaluateExample(RuleBlockInputExample(inputs = inputs))
  }

  /** Builds the mock schema and execution once, so evaluating several examples does not rebuild them. */
  private def evaluator(taskId: Identifier, ruleBlockModel: RuleBlockModel)
                       (implicit pluginContext: PluginContext): RuleBlockInputExample => Option[Value] = {
    val task = PlainTask(taskId, RuleBlockSpec(ruleBlockModel))
    val sortedPorts = ruleBlockModel.ports.sortBy(port => (port.displayOrder, port.id.toString))
    // A block has no source data of its own, so each port is fed through a mock source path.
    val mockPaths = sortedPorts.zipWithIndex.map { case (port, index) =>
      port.id -> s"ruleBlockPort${index + 1}"
    }.toMap
    val schema = EntitySchema(
      typeUri = Uri(ENTITY_TYPE),
      typedPaths = sortedPorts.flatMap { port =>
        mockPaths.get(port.id).map(path => UntypedPath.saveApply(path).asStringTypedPath)
      }
    )
    val bindingExecutions = sortedPorts.flatMap { port =>
      mockPaths.get(port.id).map { path =>
        RuleBlockBindingExecution(
          port.id,
          PathInput(
            id = Identifier(s"mock_${port.id}"),
            path = UntypedPath.saveApply(path)
          ).execution(TaskContext.noInput())
        )
      }
    }
    val execution = RuleBlockExecution(task, bindingExecutions, TaskContext.noInput())
    example => {
      val entity = Entity(
        uri = Uri(s"$ENTITY_TYPE/entity/${example.id}"),
        values = sortedPorts.map(port => example.inputs.getOrElse(port.id, Seq.empty)),
        schema = schema
      )
      execution.rootExecution.map(rootExecution => DetailedEvaluator(rootExecution, entity))
    }
  }

  private final val ENTITY_TYPE = "urn:rule-block-evaluation"
}
