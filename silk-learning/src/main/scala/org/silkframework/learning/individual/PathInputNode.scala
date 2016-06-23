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

package org.silkframework.learning.individual

import org.silkframework.entity.Path
import org.silkframework.rule.input.PathInput
import org.silkframework.util.IdentifierGenerator

case class PathInputNode(path: Path, isSource: Boolean) extends InputNode {

  def build(implicit identifiers: IdentifierGenerator) = {
    PathInput(
      id = identifiers.generate(if(isSource) "sourcePath" else "targetPath"),
      path = path
    )
  }
}

object PathInputNode {
  def load(input: PathInput, isSource: Boolean) = {
    PathInputNode(input.path, isSource)
  }
}
