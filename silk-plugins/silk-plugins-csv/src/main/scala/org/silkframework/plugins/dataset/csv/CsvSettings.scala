package org.silkframework.plugins.dataset.csv

import scala.io.Codec

case class CsvSettings(separator: Char = ',',
                       arraySeparator: Option[Char] = None,
                       quote: Option[Char] = Some('"'),
                       maxCharsPerColumn: Option[Int] = None,
                       maxColumns: Option[Int] = None,
                       commentChar: Option[Char] = None,
                       quoteEscapeChar: Char = '"',
                       linesToSkip: Int = 0,
                       nullValue: Option[String] = None,
                       codec: Codec = Codec.UTF8,
                       clearBeforeExecution: Boolean = false,
                       trimWhitespaceAndNonPrintableCharacters: Boolean = false)
