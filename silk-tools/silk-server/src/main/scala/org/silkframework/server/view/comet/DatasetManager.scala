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

package org.silkframework.server.view.comet

import xml.{Text, NodeSeq}
import net.liftweb.common.{Full, Empty, Box}
import net.liftweb.util.Helpers
import net.liftweb.http.{SHtml, CometActor}
import org.silkframework.server.model.{Dataset, Server}

class DatasetManager extends CometActor {
  override def defaultPrefix = Full("comet")

  private lazy val inputNode = Helpers.findKids(defaultXml, "comet", "input")

  private lazy val bodyNode = Helpers.findKids(defaultXml, "comet", "body")

  private lazy val rowNode = Helpers.deepFindKids(bodyNode, "comet", "row")

  override lazy val fixedRender : Box[NodeSeq] = {
    inputNode
  }

  override def render = {
    bind("comet", bodyNode, "row" -> generateRows)
  }

  private def generateRows ={
    val datasets = Server.datasets.toSeq.sortBy(_.name)

    val nodes = datasets.flatMap(generateRow)

    NodeSeq.fromSeq(nodes)
  }

  private def generateRow(dataset : Dataset) = {
    bind("row", rowNode,
         "name" -> Text(dataset.name),
         "sourceEntities" -> Text(dataset.sourceEntityCount.toString),
         "targetEntities" -> Text(dataset.targetEntityCount.toString))
  }
}
