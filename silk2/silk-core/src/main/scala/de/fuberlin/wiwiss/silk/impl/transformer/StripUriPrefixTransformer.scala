package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer

class StripUriPrefixTransformer(val params: Map[String, String] = Map()) extends Transformer
{
    override def evaluate(strings : Seq[String]) : String =
    {
        val word = strings.toList.head
        val uriPrefixEnd = math.max(word.lastIndexOf("/"), word.lastIndexOf("#"))
        if (uriPrefixEnd > -1)
            return word.substring(uriPrefixEnd + 1, word.size)
        else
            return word
    }
}
