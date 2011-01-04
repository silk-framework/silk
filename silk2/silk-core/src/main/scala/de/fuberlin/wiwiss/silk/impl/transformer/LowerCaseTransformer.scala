package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer

class LowerCaseTransformer(val params: Map[String, String] = Map()) extends Transformer
{
    override def evaluate(strings : Seq[String]) =
    {
        strings.head.toLowerCase
    }
}
