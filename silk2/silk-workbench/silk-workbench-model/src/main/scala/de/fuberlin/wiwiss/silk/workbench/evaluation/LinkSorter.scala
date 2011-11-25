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

import de.fuberlin.wiwiss.silk.workbench.workspace.TaskData

object LinkSorter extends TaskData[LinkSorter](NoSorter)
{
  def sort(links : Seq[EvalLink]) : Seq[EvalLink] =
  {
    apply()(links)
  }
}

trait LinkSorter extends (Seq[EvalLink] => Seq[EvalLink])
{
  def apply(links : Seq[EvalLink]) : Seq[EvalLink]
}

object NoSorter extends LinkSorter
{
  def apply(links : Seq[EvalLink]) = links
}

object ConfidenceSorterAscending extends LinkSorter
{
  def apply(links : Seq[EvalLink]) : Seq[EvalLink] =
  {
    links.sortBy(_.confidence.getOrElse(-1.0))
  }
}

object ConfidenceSorterDescending extends LinkSorter
{
  def apply(links : Seq[EvalLink]) : Seq[EvalLink] =
  {
    links.sortBy(-_.confidence.getOrElse(-1.0))
  }
}
