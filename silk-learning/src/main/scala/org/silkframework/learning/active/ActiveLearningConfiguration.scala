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

package org.silkframework.learning.active

import org.silkframework.learning.active.linkselector.{MaximumAgreementSelector, LinkSelectorCombinator, JensenShannonDivergenceSelector, LinkSelector}
import org.silkframework.learning.active.poolgenerator.{SimpleLinkPoolGenerator, LinkPoolGenerator}


case class ActiveLearningConfiguration(linkPoolGenerator: LinkPoolGenerator = SimpleLinkPoolGenerator(),
                                       selector: LinkSelector = ActiveLearningConfigurationDefaults.defaultLinkSelector)

object ActiveLearningConfigurationDefaults {
  val defaultLinkSelector = {
    val max = MaximumAgreementSelector()
    val jensen = JensenShannonDivergenceSelector()
    LinkSelectorCombinator(
      pickLinkSelector =  (_, _, entities) => {
        if(entities.positiveLinks.size < 2) max else jensen
      }
    )
  }
}