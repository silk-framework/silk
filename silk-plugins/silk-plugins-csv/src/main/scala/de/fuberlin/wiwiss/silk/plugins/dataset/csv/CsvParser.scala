package de.fuberlin.wiwiss.silk.plugins.dataset.csv

import com.univocity.parsers.csv.{CsvParser => UniCsvParser, CsvParserSettings}

class CsvParser(selectedIndices: Seq[Int], settings: CsvSettings) {

  private val parserSettings = new CsvParserSettings()
  import settings._
  parserSettings.getFormat.setDelimiter(separator)
  for(quoteChar <- quote) {
    parserSettings.getFormat.setQuote(quoteChar)
  }
  if(selectedIndices.nonEmpty) {
    parserSettings.selectIndexes(selectedIndices.map(new Integer(_)): _*)
  }
  maxCharsPerColumn foreach {
    parserSettings.setMaxCharsPerColumn(_)
  }
  maxColumns foreach {
    parserSettings.setMaxColumns(_)
  }
  commentChar foreach {
    parserSettings.getFormat.setComment(_)
  }

  private val parser = new UniCsvParser(parserSettings)


  def parseLine(line: String): Seq[String] = {
    parser.parseLine(line)
  }

}
