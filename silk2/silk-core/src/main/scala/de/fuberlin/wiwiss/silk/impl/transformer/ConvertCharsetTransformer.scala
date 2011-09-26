package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import java.nio.charset.Charset

@Plugin(id = "convertCharset", label = "Convert Charset", description = "Convert the string from \"sourceCharset\" to \"targetCharset\".")
case class ConvertCharsetTransformer(sourceCharset: String = "ISO-8859-1", targetCharset: String = "UTF-8") extends SimpleTransformer {
  require(Charset.isSupported(sourceCharset), "sourceCharset " + sourceCharset + " is unsupported")
  require(Charset.isSupported(targetCharset), "targetCharset " + targetCharset + " is unsupported")

  override def evaluate(value: String) = {
    val bytes = value.getBytes(sourceCharset)
    new String(bytes, targetCharset)
  }
}