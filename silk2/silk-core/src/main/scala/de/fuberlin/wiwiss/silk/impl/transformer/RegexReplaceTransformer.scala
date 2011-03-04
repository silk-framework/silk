package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import util.matching.Regex

@StrategyAnnotation(
  id = "regexReplace",
  label = "Regex replace",
  description = "Replace all occurrences of a regex \"regex\" with \"replace\" in a string.")
class RegexReplaceTransformer(regex : String, replace : String) extends Transformer
{
  private val compiledRegex = new Regex(regex)

  override def evaluate(strings : Seq[String]) =
  {
    compiledRegex.replaceAllIn(strings.head, replace)
  }
}