package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "numReduce", label = "Numeric reduce", description = "Strip all non-numeric characters from a string.")
class NumReduceTransformer() extends Transformer
{
    RegexReplaceTransformer transformer = new RegexReplaceTransformer("[^0-9]+", "")

    override def evaluate(strings : Seq[String]) =
    {
        transformer.evaluate(strings)
    }
}
