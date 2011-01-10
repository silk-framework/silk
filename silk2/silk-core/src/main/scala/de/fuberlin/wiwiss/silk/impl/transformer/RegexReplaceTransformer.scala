package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "regexReplace", label = "Regex replace", description = "Replace all occurrences of a regex \"regex\" with \"replace\" in a string.")
class RegexReplaceTransformer(regex : String, replace : String) extends Transformer
{
  override def evaluate(strings : Seq[String]) =
  {
    strings.toList.head.replaceAll(regex, replace)
  }
}