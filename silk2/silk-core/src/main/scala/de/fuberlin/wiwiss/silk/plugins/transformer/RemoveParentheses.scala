package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "removeParentheses", label = "Remove Parentheses", description = "Remove all parentheses including their content, e.g., transforms 'Berlin (City)' -> 'Berlin'.")
case class RemoveParentheses() extends RegexReplaceTransformer("""\s*\([^\)]*\)\s*""", "")
