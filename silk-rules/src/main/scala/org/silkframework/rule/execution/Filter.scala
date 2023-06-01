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

package org.silkframework.rule.execution

import org.silkframework.entity.Link
import org.silkframework.rule.LinkFilter
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}

import scala.collection.mutable.ArrayBuffer

/**
 * Filters the links according to the link limit.
 */
class Filter(links: Seq[Link], filter: LinkFilter) extends Activity[Seq[Link]] {

  override def name = "Filtering"

  override def run(context: ActivityContext[Seq[Link]])
                  (implicit userContext: UserContext): Unit = {
    filter.limit match {
      case Some(limit) => {
        context.status.updateMessage("Filtering output")
        val linkBuffer = new ArrayBuffer[Link]()
        for ((sourceUri, groupedLinks) <- links.groupBy(_.source)) {
          if(filter.unambiguous.contains(true)) {
            if(groupedLinks.distinct.size==1) {
              linkBuffer.append(groupedLinks.head)
            }
          } else {
            val bestLinks = groupedLinks.distinct.sortWith(_.confidence.getOrElse(-1.0) > _.confidence.getOrElse(-1.0)).take(limit)
            linkBuffer.appendAll(bestLinks)
          }
        }
        context.value.update(linkBuffer.toSeq)
        context.log.info("Filtered " + links.size + " links yielding " + linkBuffer.size + " links")
      }
      case None => {
        val distinctLinks = links.distinct
        context.value.update(distinctLinks)
        context.log.info("Merged " + links.size + " links yielding " + distinctLinks.size + " links")
      }
    }
  }
}
