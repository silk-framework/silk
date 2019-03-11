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

package org.silkframework.rule

import java.util.logging.Logger

import scala.xml.{Node, Text}

/**
 * A Link Filter specifies the parameters of the filtering phase.
 *
 * @param limit Defines the number of links originating from a single data item. Only the n highest-rated links per source data item will remain after the filtering.
 */
case class LinkFilter(limit: Option[Int] = None, unambiguous: Option[Boolean] = None) {
  /**
   * Serializes this Link Filter as XML.
   */
  def toXML: Node = {
    val limitXML: Option[Text] = limit match {
      case None => None
      case Some(l) => Some(Text(l.toString))
    }
    val unambiguousXML: Option[Text] = unambiguous match {
      case None => None
      case Some(u) => Some(Text(u.toString))
    }
    <Filter limit={limitXML} unambiguous={unambiguousXML} />
  }

}

object LinkFilter {
  private val logger = Logger.getLogger(LinkFilter.getClass.getName)

  def apply(limit: Int): LinkFilter = LinkFilter(Some(limit))

  /**
   * Creates a Link Filter from XML.
   */
  def fromXML(node: Node): LinkFilter = {
    val limitStr = (node \ "@limit").text
    val unambiguous = (node \ "@unambiguous").headOption.map(_.text.toBoolean)

    LinkFilter(if (limitStr.isEmpty) None else Some(limitStr.toInt), unambiguous)
  }
}
