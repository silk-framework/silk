package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "stripUriPrefix", label = "Strip URI prefix", description = "Strips the URI prefix of a string.")
class StripUriPrefixTransformer() extends Transformer
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
