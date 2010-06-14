package de.fuberlin.wiwiss.silk.transformer

import de.fuberlin.wiwiss.silk.linkspec.Transformer

class LowerCaseTransformer(val params: Map[String, String] = Map()) extends Transformer
{
    override def evaluate(strings : Seq[String]) =
    {
        strings.head.toLowerCase
    }
}
