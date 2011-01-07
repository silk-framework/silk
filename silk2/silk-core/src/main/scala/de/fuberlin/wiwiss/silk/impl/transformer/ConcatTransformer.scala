package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "concat", label = "Concatenate", description = "Concatenates strings from two inputs.")
class ConcatTransformer(glue : String = "") extends Transformer
{
    override def evaluate(strings : Seq[String]) =
    {
        (strings.head /: strings.tail) (_ + glue + _)
    }
}