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

package de.fuberlin.wiwiss.silk.config

import xml.Node
import java.util.logging.Logger

/**
 * A Link Filter specifies the parameters of the filtering phase.
 *
 * @param threshold Defines the minimum similarity of two data items which is required to generate a link between them.
 * @param limit Defines the number of links originating from a single data item. Only the n highest-rated links per source data item will remain after the filtering.
 */
case class LinkFilter(limit: Option[Int] = None, threshold: Option[Double] = None) {
  /**
   * Serializes this Link Filter as XML.
   */
  def toXML: Node = limit match {
    case Some(l) => <Filter limit={l.toString}/>
    case None => <Filter/>
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
    val threshold = (node \ "@threshold").headOption.map(_.text.toDouble)

    if (threshold.isDefined) {
      logger.warning("The use of a global threshold is deprecated. Please use per-comparison thresholds.")
    }

    LinkFilter(if (limitStr.isEmpty) None else Some(limitStr.toInt), threshold)
  }
}
