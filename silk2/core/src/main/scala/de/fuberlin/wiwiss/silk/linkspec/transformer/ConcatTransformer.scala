package de.fuberlin.wiwiss.silk.transformer

import de.fuberlin.wiwiss.silk.linkspec.Transformer

class ConcatTransformer(val params: Map[String, String] = Map()) extends Transformer
{
    val glue = params("glue")
    
    override def evaluate(strings : Seq[String]) =
    {
        (strings.head /: strings.tail) (_ + glue + _)
    }
}