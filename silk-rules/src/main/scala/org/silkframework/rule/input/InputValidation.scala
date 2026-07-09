package org.silkframework.rule.input

import org.silkframework.runtime.validation.ValidationException

object InputValidation {

  /**
   * Rejects input-port placeholders in ordinary transform/linking input trees.
   * Input ports are only valid inside rule block definitions.
   */
  def validateNoInputPortsOutsideRuleBlocks(input: Input, validationException: => ValidationException): Unit = {
    input match {
      case _: InputPortInput =>
        throw validationException
      case TransformInput(_, _, inputs) =>
        inputs.foreach(validateNoInputPortsOutsideRuleBlocks(_, validationException))
      case RuleBlockInput(_, _, bindings) =>
        bindings.foreach(binding => validateNoInputPortsOutsideRuleBlocks(binding.input, validationException))
      case _: PathInput =>
    }
  }
}
