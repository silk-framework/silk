/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.rule.plugins.transformer.substring

import java.net.URLDecoder
import java.util.logging.{Level, Logger}

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.{Plugin, TransformExample, TransformExamples}
import org.silkframework.util.Uri

import scala.math.max

@Plugin(
  id = "stripUriPrefix",
  categories = Array("Substring", "Normalize"),
  label = "Strip URI prefix",
  description = "Strips the URI prefix and decodes the remainder. Leaves values unchanged which are not a valid URI."
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("http://example.org/some/path/to/value"),
    output = Array("value")
  ),
  new TransformExample(
    input1 = Array("urn:scheme:value"),
    output = Array("value")
  ),
  new TransformExample(
    input1 = Array("http://example.org/some/path/to/encoded%20v%C3%A4lue"),
    output = Array("encoded välue")
  ),
  new TransformExample(
    input1 = Array("value"),
    output = Array("value")
  )
))
case class StripUriPrefixTransformer() extends SimpleTransformer {

  @transient
  private val log = Logger.getLogger(classOf[StripUriPrefixTransformer].getName)

  override def evaluate(value: String): String = {
    if(new Uri(value).isValidUri) {
      //Remove prefix
      var uriPrefixEnd = max(value.lastIndexOf("/"), value.lastIndexOf("#"))
      uriPrefixEnd = max(uriPrefixEnd, value.lastIndexOf(":"))
      val remainder = value.substring(uriPrefixEnd + 1)

      //Decode url
      val clean = remainder.replace('_', ' ')
      try {
        URLDecoder.decode(clean, "utf8")
      }
      catch {
        case ex: Exception => {
          log.log(Level.FINE, "Bad URI", ex)
          clean
        }
      }
    }
    else {
      value
    }
  }
}
