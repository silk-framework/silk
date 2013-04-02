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

package de.fuberlin.wiwiss.silk.workbench.evaluation

import de.fuberlin.wiwiss.silk.workspace.TaskData
import EvalLink._

object LinkSorter extends TaskData[LinkSorter](NoSorter) {
  def sort(links: Seq[EvalLink]): Seq[EvalLink] = {
    apply()(links)
  }
}

trait LinkSorter extends (Seq[EvalLink] => Seq[EvalLink]) {
  def apply(links: Seq[EvalLink]): Seq[EvalLink]
}

object NoSorter extends LinkSorter {
  def apply(links: Seq[EvalLink]) = links
}

object SourceUriSorterAscending extends LinkSorter {
  def apply(links: Seq[EvalLink]) = links.sortBy(_.source)
}

object SourceUriSorterDescending extends LinkSorter {
  def apply(links: Seq[EvalLink]) = links.sortBy(_.source).reverse
}

object TargetUriSorterAscending extends LinkSorter {
  def apply(links: Seq[EvalLink]) = links.sortBy(_.target)
}

object TargetUriSorterDescending extends LinkSorter {
  def apply(links: Seq[EvalLink]) = links.sortBy(_.target).reverse
}

object ConfidenceSorterAscending extends LinkSorter {
  def apply(links: Seq[EvalLink]): Seq[EvalLink] = {
    links.sortBy(_.confidence.getOrElse(-1.0))
  }
}

object ConfidenceSorterDescending extends LinkSorter {
  def apply(links: Seq[EvalLink]): Seq[EvalLink] = {
    links.sortBy(-_.confidence.getOrElse(-1.0))
  }
}

object CorrectnessSorterAscending extends LinkSorter {
  def apply(links: Seq[EvalLink]) = {
    links.sortBy{ _.correct match {
      case Correct => 0
      case Incorrect => 1
      case Unknown => 2
    }}
  }
}

object CorrectnessSorterDescending extends LinkSorter {
  def apply(links: Seq[EvalLink]) = {
    links.sortBy{ _.correct match {
      case Unknown => 0
      case Correct => 1
      case Incorrect => 2
    }}
  }
}
