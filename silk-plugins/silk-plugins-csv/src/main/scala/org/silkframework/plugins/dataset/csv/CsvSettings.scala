package org.silkframework.plugins.dataset.csv

import org.silkframework.runtime.plugin.Param

case class CsvSettings(separator: Char = ',',
                       arraySeparator: Option[Char] = None,
                       quote: Option[Char] = Some('"'),
                       maxCharsPerColumn: Option[Int] = None,
                       maxColumns: Option[Int] = None,
                       commentChar: Option[Char] = None,
                       nullValue: Option[String] = None,
                       treatEmptyAsNull: Boolean = false)
