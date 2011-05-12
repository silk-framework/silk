package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import java.nio.charset.Charset

@StrategyAnnotation(id = "convertCharset", label = "Convert Charset", description = "Convert the string from \"sourceCharset\" to \"targetCharset\".")
case class ConvertCharsetTransformer(sourceCharset : String = "ISO-8859-1", targetCharset : String = "UTF-8") extends Transformer
{
  require (Charset.isSupported(sourceCharset), "sourceCharset " + sourceCharset + " is unsupported")
  require (Charset.isSupported(targetCharset), "targetCharset " + targetCharset + " is unsupported")

  override def evaluate(strings : Seq[String]) =
  {
    val bytes = strings.head.getBytes(sourceCharset)
    new String(bytes, targetCharset)
  }
}