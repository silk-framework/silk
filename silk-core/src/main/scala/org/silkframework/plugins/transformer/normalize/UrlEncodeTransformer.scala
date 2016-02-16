package org.silkframework.plugins.transformer.normalize

import java.net.URLEncoder

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin

/**
 * Created by andreas on 2/16/16.
 */
@Plugin(
  id = "urlEncode",
  categories = Array("Normalize"),
  label = "URL Encode",
  description = "URL encodes the string."
)
case class UrlEncodeTransformer(encoding: String = "UTF-8") extends SimpleTransformer {
  override def evaluate(value: String) = {
    URLEncoder.encode(value, encoding)
  }
}
