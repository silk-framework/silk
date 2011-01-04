package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer

class RegexReplaceTransformer(val params: Map[String, String] = Map()) extends Transformer
{
    require(params.contains("regex"), "Parameter 'regex' is required")
    require(params.contains("replace"), "Parameter 'replace' is required")

    val regex = params("regex")
    val replace = params("replace")

    override def evaluate(strings : Seq[String]) =
    {
        strings.toList.head.replaceAll(regex, replace)
    }
}