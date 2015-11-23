package org.silkframework.plugins.dataset.csv

case class CsvSettings(separator: Char = ',',
                       arraySeparator: Option[Char] = None,
                       quote: Option[Char] = None,
                       maxCharsPerColumn: Option[Int] = None,
                       maxColumns: Option[Int] = None,
                       commentChar: Option[Char] = None) {

}
