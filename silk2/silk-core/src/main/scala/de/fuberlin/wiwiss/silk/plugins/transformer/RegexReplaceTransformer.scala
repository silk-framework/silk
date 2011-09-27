package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import util.matching.Regex

@Plugin(
  id = "regexReplace",
  label = "Regex replace",
  description = "Replace all occurrences of a regex \"regex\" with \"replace\" in a string.")
class RegexReplaceTransformer(regex: String, replace: String) extends SimpleTransformer {
  private val compiledRegex = new Regex(regex)

  override def evaluate(value: String) = {
    compiledRegex.replaceAllIn(value, replace)
  }
}