package de.fuberlin.wiwiss.silk.plugins.dataset.csv

import com.univocity.parsers.csv.{CsvParser => UniCsvParser, CsvParserSettings}

class CsvParser(selectedIndices: Seq[Int], settings: CsvSettings) {

  private val parserSettings = new CsvParserSettings()
  parserSettings.getFormat.setDelimiter(settings.separator)
  for(quoteChar <- settings.quote)
    parserSettings.getFormat.setQuote(quoteChar)
  if(selectedIndices.nonEmpty)
    parserSettings.selectIndexes(selectedIndices.map(new Integer(_)): _*)

  private val parser = new UniCsvParser(parserSettings)

  def parseLine(line: String): Seq[String] = {
    parser.parseLine(line)
  }

}
