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

import org.silkframework.learning.active.linkselector._
import org.silkframework.learning.active.poolgenerator.{CombinedLinkPoolGenerator, IndexLinkPoolGenerator, LinkPoolGenerator, LinkSpecLinkPoolGenerator}


case class ActiveLearningConfiguration(linkPoolGenerator: LinkPoolGenerator = ActiveLearningConfiguration.defaultLinkPoolGenerator,
                                       selector: LinkSelector = ActiveLearningConfiguration.defaultLinkSelector)

object ActiveLearningConfiguration {

  val defaultLinkPoolGenerator: LinkPoolGenerator = {
    new CombinedLinkPoolGenerator(
      new LinkSpecLinkPoolGenerator(),
      new IndexLinkPoolGenerator()
    )
  }

  val defaultLinkSelector: LinkSelector = {
    // We sample the rules and pool links to limit the runtime of the selectors
    val max = SamplingLinkSelector(BestMatchSelector(), linkSampleSize = Some(5000), ruleSampleSize = Some(400))
    val jensen = SamplingLinkSelector(JensenShannonDivergenceSelector(), linkSampleSize = Some(1000), ruleSampleSize = Some(1000))
    //val optimized = SamplingLinkSelector(OptimizedSelector(), linkSampleSize = Some(5000), ruleSampleSize = Some(1000))
    LinkSelectorCombinator(
      pickLinkSelector =  (_, referenceData) => {
        if(referenceData.positiveLinks.size < 2) max else jensen
      }
    )
  }
}
