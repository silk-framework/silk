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

package de.fuberlin.wiwiss.silk.execution

import de.fuberlin.wiwiss.silk.dataset.{Dataset}
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.util.Uri
import de.fuberlin.wiwiss.silk.runtime.task.Task

/**
 * Writes the links to the output.
 */
class OutputTask(links: Seq[Link], linkType: Uri, outputs: Traversable[Dataset]) extends Task[Unit] {
  taskName = "Writing output"

  override def execute() {
    val sinks = outputs.map(_.sink)

    sinks.foreach(_.open)

    for (link <- links;
         sink <- sinks) {
      sink.write(link, linkType.toString)
    }

    sinks.foreach(_.close)
  }
}