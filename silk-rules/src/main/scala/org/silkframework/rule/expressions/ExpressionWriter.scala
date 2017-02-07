package org.silkframework.rule.expressions

import org.silkframework.config.Prefixes
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer

/**
  * Given a rule tree, generates an expression.
  */
object ExpressionWriter {

  def apply(op: Input)(implicit prefixes: Prefixes): String = op match {
    case PathInput(id, path) =>
      path.serializeSimplified(prefixes)
    case TransformInput(id, ConstantTransformer(value), Seq()) =>
      value
    case TransformInput(id, transformer, inputs) =>
      val str = new StringBuilder

      // Function name
      str ++= transformer.plugin.id.toString

      // Function parameters
      if(transformer.plugin.parameters.nonEmpty) {
        val parameters =
          for((key, value) <- transformer.parameters) yield {
            key + ':' + value
          }
        str ++= parameters.mkString("[", ";", "]")
      }

      // Input parameters
      str ++= inputs.map(apply).mkString("(", ";", ")")

      str.toString()
  }

}
