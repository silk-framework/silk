package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer

class StripPostfixTransformer(val params: Map[String, String] = Map()) extends Transformer
{
    require(params.contains("postfix"), "Parameter 'postfix' is required")

    val postfix = params("postfix")
    
    override def evaluate(strings : Seq[String]) : String =
    {
        val word = strings.toList.head
        if (word.endsWith(postfix))
            return word.substring(0, postfix.size)
        else
            return word
    }
}