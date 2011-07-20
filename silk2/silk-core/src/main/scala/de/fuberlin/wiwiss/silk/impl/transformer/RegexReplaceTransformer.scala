package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import util.matching.Regex

@StrategyAnnotation(
  id = "regexReplace",
  label = "Regex replace",
  description = "Replace all occurrences of a regex \"regex\" with \"replace\" in a string.")
class RegexReplaceTransformer(regex: String, replace: String) extends SimpleTransformer {
  private val compiledRegex = new Regex(regex)

  override def evaluate(value: String) = {
    compiledRegex.replaceAllIn(value, replace)
  }
}