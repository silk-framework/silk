package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "alphaReduce", label = "Alpha reduce", description = "Strips all non-alphabetic characters from a string.")
class AlphaReduceTransformer() extends Transformer
{
    RegexReplaceTransformer transformer = new RegexReplaceTransformer("[^\\pL]+", "")

    override def evaluate(strings : Seq[String]) =
    {
        transformer.evaluate(strings)
    }
}