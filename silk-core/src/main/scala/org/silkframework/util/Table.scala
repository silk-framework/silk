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

package org.silkframework.util

case class Table(name: String,
                 header: Seq[String],
                 values: Seq[Seq[Any]])
                (columnWidthInCharacters: Seq[Int] = Table.defaultColumnSizes(header.size)) {

  def transpose = Table(name, header, values.transpose)(columnWidthInCharacters)

  /**
   * Formats this table as CSV.
   */
  def toCsv = {
    val csv = new StringBuilder()

    csv.append(name + "," + header.mkString(",") + "\n")
    for(row <- values)
      csv.append(row.mkString(",") + "\n")

    csv.toString()
  }

  /**
   * Formats this table as textile.
   */
  def toTextile = {
    val sb = new StringBuilder()

    sb.append(header.mkString("|_. ", " |_. ", " |\n"))
    for(row <- values) {
      sb.append("| " + row.mkString(" | ") + " |\n")
    }

    sb.toString()
  }

  /**
   * Formats this table as markdown.
   */
  def toMarkdown = {
    val sb = new StringBuilder()

    sb.append(header.mkString("| ", " | ", " |\n"))
    sb.append("| " + (" --- |" * header.size) + "\n")
    for(row <- values) {
      // If there are line breaks in a value, we need to generate multiple rows
      val rowLines = row.zip(columnWidthInCharacters).map { case (v, maxChars) =>
        Table.softGrouped(v.toString.replace("\\", "\\\\"), maxChars).flatMap(_.split("[\n\r]+"))
      }
      val maxLines = rowLines.map(_.length).max

      for(index <- 0 until maxLines) {
        val lineValues = rowLines.map(lines => if(index >= lines.length) "" else lines(index))
        sb.append("| " + lineValues.mkString(" | ") + " |\n")
      }
    }

    sb.toString()
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
    for(row <- values)
      sb.append(row.mkString(" & ") + "\\\\\n")
    sb.append("\\hline\n")
    sb.append("\\end{tabular}\n")
    sb.append("\\caption{" + name + "}\n")
    sb.append("%\\label{}\n")
    sb.append("\\end{table}\n")

    sb.toString()
  }
}

object Table {

  // Similar to String.grouped, but tries to split Strings on whitespace characters
  def softGrouped(input: String, maxLength: Int): Seq[String] = {
    assert(maxLength > 0)
    val minLength = math.max(1, (maxLength * (1.0 / 2)).toInt)
    var remainingString = input
    var splits = Vector.empty[String]
    while(remainingString.size > 0) {
      val whiteSpaceSplitIdx = remainingString.take(maxLength + 1).lastIndexOf(' ')
      val camelCaseSplitIdx = remainingString.take(maxLength + 1).sliding(2).toSeq.lastIndexWhere(s => s.length == 2 && s(0).isLower && s(1).isUpper) + 1

      val splitIndex = if(whiteSpaceSplitIdx > minLength) {
        whiteSpaceSplitIdx
      } else if(camelCaseSplitIdx > minLength) {
        camelCaseSplitIdx
      } else {
        maxLength
      }

      val (next, remain) = remainingString.splitAt(splitIndex)
      splits :+= next
      remainingString = remain
    }
    splits
  }

  def defaultColumnSizes(nrColumns: Int): Seq[Int] = {
    val columnSize = 180 / nrColumns
    for(i <- 1 to nrColumns) yield {
      columnSize
    }
  }
}