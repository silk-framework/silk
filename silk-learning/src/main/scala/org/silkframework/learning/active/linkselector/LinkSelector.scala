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

package org.silkframework.learning.active.linkselector

import org.silkframework.entity.Link
import org.silkframework.evaluation.ReferenceEntities

/**
 * Selects a link from the unlabeled pool for evaluation by the user.
 * An implementation should select the most informative links e.g. the links for which the current linkage rules are most uncertain.
 */
trait LinkSelector {
  /**
   * Selects a link from the unlabeled pool for evaluation by the user.
   *
   * @param rules The current linkage rules which have been trained on the provided reference links
   * @param unlabeledLinks The pool of unlabeled links from which a set of links should be selected
   * @param referenceEntities The current reference links
   *
   * @return A sequence of links which is ordered by informativeness. The number of links which are returned depends on the implementation.
   */
  def apply(rules: Seq[WeightedLinkageRule], unlabeledLinks: Seq[Link], referenceEntities: ReferenceEntities): Seq[Link]
}
