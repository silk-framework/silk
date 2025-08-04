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

import org.silkframework.config.Prefixes
import org.silkframework.dataset.LinkSink
import org.silkframework.entity.Link
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.Uri

/**
 * Writes the links to the output.
 */
class OutputWriter(links: Seq[Link], linkType: Uri, inversePredicateUri: Option[Uri], output: Option[LinkSink])
                  (implicit prefixes: Prefixes) extends Activity[Unit] {

  override def name = "Writing output"

  override def run(context: ActivityContext[Unit])
                  (implicit userContext: UserContext): Unit = {
    output.foreach(_.clear())
    output.foreach(_.init())

    for (link <- links;
         output <- output) {
      output.writeLink(link, linkType.toString, inversePredicateUri.map(_.uri))
    }

    output.foreach(_.close())
  }
}