package de.fuberlin.wiwiss.silk.transformer

import de.fuberlin.wiwiss.silk.linkspec.Transformer

class StripPrefixTransformer(val params: Map[String, String] = Map()) extends Transformer
{
    require(params.contains("prefix"), "Parameter 'prefix' is required")

    val prefix = params("prefix")

    override def evaluate(strings : Seq[String]) : String =
    {
        val word = strings.toList.head
        if (word.startsWith(prefix))
            return word.substring(prefix.size, word.size)
        else
            return word        
    }
}