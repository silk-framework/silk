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

package org.silkframework.config

import scala.xml.Node

/**
 * The configuration of the blocking feature.
 *
 * @param isEnabled Enable/disable blocking.
 * @param blocks The number of blocks. Keeping the default is recommended.
 */
case class Blocking(isEnabled: Boolean = true, blocks: Int = Blocking.DefaultBlocks) {
  require(blocks > 0, "blocks > 0")

  /**
   * Returns the number of blocks if blocking is enabled or 1 if blocking is disabled.
   */
  def enabledBlocks = if (isEnabled) blocks else 1

  def toXML: Node = {
      <Blocking enabled={isEnabled.toString} blocks={blocks.toString}/>
  }
}

object Blocking {
  val DefaultBlocks = 101

  def fromXML(node: Node): Blocking = {
    val enabled = (node \ "@enabled").headOption.map(_.text.toBoolean) match {
      case Some(e) => e
      case None => true
    }

    val blocks = (node \ "@blocks").headOption.map(_.text.toInt).getOrElse(DefaultBlocks)

    Blocking(enabled, blocks)
  }
}
