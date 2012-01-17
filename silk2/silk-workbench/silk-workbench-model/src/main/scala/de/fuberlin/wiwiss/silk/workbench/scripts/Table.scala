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

package de.fuberlin.wiwiss.silk.workbench.scripts

case class Table(name: String, header: Seq[String], rows: Seq[String], values: Seq[Seq[Any]]) {

  def transpose = Table(name, rows, header, values.transpose)

  /**
   * Formats this table as CSV.
   */
  def toCsv = {
    val csv = new StringBuilder()

    csv.append(name + "," + header.mkString(",") + "\n")
    for((label, row) <- rows zip values)
      csv.append(label + "," + row.mkString(",") + "\n")

    csv.toString
  }

  /**
   * Formats this table as latex.
   */
  def toLatex = {
    val sb = new StringBuilder()

    sb.append("\\begin{table}\n")
    sb.append("\\begin{tabular}{|l|" + header.map(_ => "c").mkString("|") + "|}\n")
    sb.append("\\hline\n")
    sb.append(" & " + header.mkString(" & ") + "\\\\\n")
    sb.append("\\hline\n")
    for((label, row) <- rows zip values)
      sb.append(label + " & " + row.mkString(" & ") + "\\\\\n")
    sb.append("\\hline\n")
    sb.append("\\end{tabular}\n")
    sb.append("\\caption{" + name + "}\n")
    sb.append("%\\label{}\n")
    sb.append("\\end{table}\n")

    sb.toString
  }
}