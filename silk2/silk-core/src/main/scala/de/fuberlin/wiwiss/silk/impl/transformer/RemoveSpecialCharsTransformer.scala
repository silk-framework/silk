package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "removeSpecialChars", label = "Remove special chars", description = "Remove special characters (including punctuation) from a string.")
class RemoveSpecialCharsTransformer() extends Transformer
{
    RegexReplaceTransformer transformer = new RegexReplaceTransformer("[^\\d\\pL\\w]+", "")

    override def evaluate(strings : Seq[String]) =
    {
        transformer.evaluate(strings)
    }
}