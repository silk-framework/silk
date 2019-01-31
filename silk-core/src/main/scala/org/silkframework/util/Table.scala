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

import java.util.logging.Logger

case class Table(name: String,
                 header: Seq[String],
                 values: Seq[Seq[Any]])
                (columnWidthInCharacters: Seq[Int] = Table.defaultColumnSizes(header.size)) {
  private val logger: Logger = Logger.getLogger(this.getClass.getName)

  def transpose: Table = Table(name, header, values.transpose)(columnWidthInCharacters)

  /**
   * Formats this table as CSV.
   */
  def toCsv: String = {
    val csv = new StringBuilder()

    csv.append(name + "," + header.mkString(",") + "\n")
    for(row <- values)
      csv.append(row.mkString(",") + "\n")

    csv.toString()
  }

  /**
   * Formats this table as textile.
   */
  def toTextile: String = {
    val sb = new StringBuilder()

    sb.append(header.mkString("|_. ", " |_. ", " |\n"))
    for(row <- values) {
      sb.append("| " + row.mkString(" | ") + " |\n")
    }

    sb.toString()
  }

  /**
    * Formats this table as markdown.
    * This is how multiline tables should look like:

|  First column  | Second column                                                | Third column          |
|-------------------------|----------------------------------------------------|---------------------|
| Cell content  | This cell holds some more text content.\
                  The row exceeds the maximum count of chars.\
                  \
                  Third line.                                                  | The next cell starts\
                                                                                 after the closing pipe\
                                                                                 symbol.             |

    */
  def toMarkdown: String = {
    val MAX_CHARACTERS = 500
    val sb = new StringBuilder()

    sb.append(header.mkString("| ", " | ", " |\n"))
    sb.append("| " + (" --- |" * header.size) + "\n")
    for(row <- values) {
      sb.append("| ")
      val lineValues = scala.collection.mutable.ListBuffer.empty[String]
      for(cell <- row.zip(columnWidthInCharacters)) {
        val cellValue = {
          val value = cell._1
          var truncatedValue = if(value != null) value.toString.take(MAX_CHARACTERS) else "*null*"
          val newLineIdx = truncatedValue.lastIndexOf('\n')
          if(truncatedValue.length >= MAX_CHARACTERS && newLineIdx > 0) {
            logger.warning(s"Had to truncate cell value of in table $name because it exceeded the max length of $MAX_CHARACTERS characters.")
            truncatedValue = truncatedValue.take(newLineIdx) + "\n…\n"
          }
          truncatedValue.replace("\\", "\\\\")
        }
        val maxChars = cell._2
        // If there are line breaks in a value, we need to generate multiple rows
        val lines = Table.softGrouped(cellValue, maxChars)
        lineValues += lines.mkString("\\\n")
      }
      sb.append(lineValues.mkString(" | "))
      sb.append(" |\n")
    }

    sb.toString()
  }

  /**
   * Formats this table as latex.
   */
  def toLatex: String = {
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
    while(remainingString.length > 0) {
      val newLineSplitIdx = remainingString.take(maxLength + 1).indexOf('\n')
      val whiteSpaceSplitIdx = remainingString.take(maxLength + 1).lastIndexOf(' ')
      val slashSplitIdx = remainingString.take(maxLength + 1).lastIndexOf('/') + 1
      val camelCaseSplitIdx = remainingString.take(maxLength + 1).sliding(2).toSeq.lastIndexWhere(s => s.length == 2 && s(0).isLower && s(1).isUpper) + 1
      var isCamelCase = false
      var isNewLine = false

      val splitIndex = if(newLineSplitIdx >= 0) {
        isNewLine = true
        newLineSplitIdx + 1  // Newline should be included at the end
      } else if(whiteSpaceSplitIdx > minLength) {
        whiteSpaceSplitIdx
      } else if(slashSplitIdx > minLength) {
        slashSplitIdx
      } else if(camelCaseSplitIdx > minLength) {
        isCamelCase = true
        camelCaseSplitIdx
      } else {
        maxLength
      }

      val (next, remain) = remainingString.splitAt(splitIndex)
      val camelCaseSuffix = if(isCamelCase) "-" else ""
      splits :+= next.stripSuffix("\n").stripSuffix("\r") + camelCaseSuffix
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