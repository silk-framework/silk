package org.silkframework.plugins.transformer.normalize

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin

import scala.util.matching.Regex

@Plugin(
  id = "removeParentheses",
  categories = Array("Normalize"),
  label = "Remove Parentheses",
  description = "Remove all parentheses including their content, e.g., transforms 'Berlin (City)' -> 'Berlin'."
)
case class RemoveParentheses() extends SimpleTransformer {
  private val compiledRegex = new Regex("""\s*\([^\)]*\)\s*""")

  def evaluate(value: String) = {
    compiledRegex.replaceAllIn(value, "")
  }
}