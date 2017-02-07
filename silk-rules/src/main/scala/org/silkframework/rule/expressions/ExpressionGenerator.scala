package org.silkframework.rule.expressions

import org.silkframework.config.Prefixes
import org.silkframework.entity.Path
import org.silkframework.rule.input.{Input, PathInput, TransformInput, Transformer}
import org.silkframework.rule.plugins.transformer.numeric.NumOperationTransformer
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.util.IdentifierGenerator

/**
  * Generates operator trees for expressions.
  */
class ExpressionGenerator {

  val identifiers = new IdentifierGenerator

  def constant(value: String) = {
    TransformInput(
      id = identifiers.generate("value"),
      transformer = ConstantTransformer(value)
    )
  }

  def path(pathStr: String)(implicit prefixes: Prefixes) = {
    PathInput(
      id = identifiers.generate("path"),
      path = Path.parse(pathStr)
    )
  }

  def numOp(input1: Input, op: String, input2: Input) = {
    TransformInput(
      id = identifiers.generate("operator"),
      transformer = NumOperationTransformer(op),
      inputs = List(input1, input2)
    )
  }

  def func(transformer: Transformer, input: Input) = {
    TransformInput(
      id = identifiers.generate("function"),
      transformer = transformer,
      inputs = List(input)
    )
  }

  def func(transformer: Transformer, inputs: Seq[Input]) = {
    TransformInput(
      id = identifiers.generate("function"),
      transformer = transformer,
      inputs = inputs
    )
  }

}
