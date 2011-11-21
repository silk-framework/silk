/* 
 * Copyright 2011 Freie Universität Berlin, MediaEvent Services GmbH & Co. KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workbench.evaluation

import de.fuberlin.wiwiss.silk.workbench.workspace.TaskData
import de.fuberlin.wiwiss.silk.linkagerule.evaluation.DetailedLink._

object LinkFilter extends TaskData[String]("") {
  def filter(links : Seq[EvalLink]) : Seq[EvalLink] = {
    val value = apply().trim.toLowerCase

    if(value.isEmpty)
      links
    else
      links.filter(new LinkFilter(value))
  }
}

class LinkFilter(value: String) extends (EvalLink => Boolean) {
  def apply(link: EvalLink): Boolean = {
    link.source.toLowerCase.contains(value) ||
    link.target.toLowerCase.contains(value) ||
    (link.details match {
      case Some(details) => hasValue(details)
      case None => false
    })
  }

  private def hasValue(similarity: Confidence): Boolean = similarity match {
    case AggregatorConfidence(_, _, children) => children.exists(hasValue)
    case ComparisonConfidence(_, _, i1, i2) => {
      i1.values.exists(_.toLowerCase.contains(value)) ||
      i2.values.exists(_.toLowerCase.contains(value))
    }
    case SimpleConfidence(_) => false
  }
}