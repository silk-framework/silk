package de.fuberlin.wiwiss.silk.transformer

import de.fuberlin.wiwiss.silk.linkspec.Transformer

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