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

package de.fuberlin.wiwiss.silk.plugins.transformer.substring

import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import math.max
import java.net.URLDecoder
import java.util.logging.{Level, Logger}

@Plugin(
  id = "stripUriPrefix",
  categories = Array("substring"),
  label = "Strip URI prefix",
  description = "Strips the URI prefix and decodes the remainder. Leaves values unchanged which don't start with 'http:'"
)
case class StripUriPrefixTransformer() extends SimpleTransformer {
  private val log = Logger.getLogger(classOf[StripUriPrefixTransformer].getName)

  override def evaluate(value: String): String = {
    if(value.startsWith("http:")) {
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
