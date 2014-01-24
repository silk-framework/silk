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

package de.fuberlin.wiwiss.silk.linkagerule.input

import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.config.Prefixes
import xml.Node
import de.fuberlin.wiwiss.silk.linkagerule.Operator
import de.fuberlin.wiwiss.silk.util.plugin.ResourceLoader

/**
 * An input that retrieves a set of values.
 */
trait Input extends Operator {
  /**
   * Retrieves the values of this input for a given entity.
   *
   * @param entities The pair of entities.
   * @return The values.
   */
  def apply(entities: DPair[Entity]): Set[String]

  def toXML(implicit prefixes: Prefixes): Node
}

object Input {

  def fromXML(nodes: Seq[Node], resourceLoader: ResourceLoader)(implicit prefixes: Prefixes): Seq[Input] = {
    nodes.collect {
      case node @ <Input/> => PathInput.fromXML(node)
      case node @ <TransformInput>{_*}</TransformInput> => TransformInput.fromXML(node, resourceLoader)
    }
  }
}