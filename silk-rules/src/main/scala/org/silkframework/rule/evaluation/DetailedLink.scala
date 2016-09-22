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

package org.silkframework.rule.evaluation

import org.silkframework.entity.{Entity, Link}
import org.silkframework.util.DPair

class DetailedLink(source: String,
                   target: String,
                   entities: Option[DPair[Entity]],
                   val details: Option[Confidence]) extends Link(source, target, details.flatMap(_.score), entities) {

  def this(link: Link) = this(link.source, link.target, link.entities, link.confidence.map(c => SimpleConfidence(Some(c))))

  override def toXML =
    <DetailedLink source={source} target={target}>
      { details.map(_.toXML).toList }
    </DetailedLink>
}