package de.fuberlin.wiwiss.silk.plugins.transformer.normalize

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.plugins.transformer.replace.RegexReplaceTransformer

@Plugin(
  id = "removeParentheses",
  categories = Array("Normalize"),
  label = "Remove Parentheses",
  description = "Remove all parentheses including their content, e.g., transforms 'Berlin (City)' -> 'Berlin'."
)
case class RemoveParentheses() extends RegexReplaceTransformer("""\s*\([^\)]*\)\s*""", "")
