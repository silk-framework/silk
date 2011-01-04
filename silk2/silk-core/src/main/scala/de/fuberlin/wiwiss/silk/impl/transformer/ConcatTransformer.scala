package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer

class ConcatTransformer(val params: Map[String, String] = Map()) extends Transformer
{
    val glue = readOptionalParam("glue").getOrElse("")
    
    override def evaluate(strings : Seq[String]) =
    {
        (strings.head /: strings.tail) (_ + glue + _)
    }
}