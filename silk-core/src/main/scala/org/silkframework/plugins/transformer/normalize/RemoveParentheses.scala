package org.silkframework.plugins.transformer.normalize

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.plugins.transformer.replace.{RegexReplaceTransformerBase, RegexReplaceTransformer}

@Plugin(
  id = "removeParentheses",
  categories = Array("Normalize"),
  label = "Remove Parentheses",
  description = "Remove all parentheses including their content, e.g., transforms 'Berlin (City)' -> 'Berlin'."
)
case class RemoveParentheses() extends RegexReplaceTransformerBase("""\s*\([^\)]*\)\s*""", "")
