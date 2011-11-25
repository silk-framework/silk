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

package de.fuberlin.wiwiss.silk.learning

case class ResultTable(header: Seq[String], values: Seq[Seq[String]]) {

  def toCsv = {
    header.mkString("", ",", "\n") + values.map(_.mkString("", ",", "\n")).mkString
  }

  def toLatex = {
    "\\begin{tabular}{| l | l | c | c | c | c |}\n" +
    "\\hline\n" +
    header.mkString("", " & ", "\\\\\n") +
    "\\hline\n" +
    values.map(_.mkString("", " & ", "\\\\\n")).mkString +
    "\\hline\n" +
    "\\end{tabular}\n"
  }
}