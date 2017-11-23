package org.silkframework.plugins.dataset.csv

import scala.io.Codec

/**
  * Properties shared by all CSV like datasets.
  */
trait CsvDatasetTrait {
  /** The character that is used to separate values. If not provided, should default to ',', i.e., comma-separated values.
    * "\t" for specifying tab-separated values, should also be supported. */
  def separator: String
  /** The character that is used to separate the parts of array values. Should accept literal "\t" to specify the tab character. */
  def arraySeparator: String
  /** Character used to quote values, should default to '"' */
  def quote: String
  /**  A URI prefix that should be used for generating schema entities like classes or properties, e.g. http://www4.wiwiss.fu-berlin.de/ontology/ */
  def prefix: String
  /** The file encoding, e.g., UTF8, ISO-8859-1. Should default to "UTF-8" */
  def charset: String
  /** The number of lines to skip in the beginning, e.g. copyright, meta information etc. Should default to zero. */
  def linesToSkip: Int
  /** The maximum characters per column. If there are more characters found, the parser will fail. */
  def maxCharsPerColumn: Int
  /** If set to true then the parser will ignore lines that have syntax errors or do not have to correct number of fields
    * according to the current config. Should default to false. */
  def ignoreBadLines: Boolean
  /** Escape character to be used inside quotes, used to escape the quote character. It must also be used to escape itself,
    * e.g. by doubling it, e.g. \"\". If left empty, it should default to quote. */
  def quoteEscapeCharacter: String

  val separatorChar: Char =
    if (separator == "\\t") { '\t' }
    else if (separator.length == 1) { separator.head }
    else { throw new IllegalArgumentException(s"Invalid separator: '$separator'. Must be a single character.") }

  val arraySeparatorChar: Option[Char] =
    if (arraySeparator.isEmpty) { None }
    else if(arraySeparator == "\\t") { Some('\t') }
    else if (arraySeparator.length == 1) { Some(arraySeparator.head) }
    else { throw new IllegalArgumentException(s"Invalid array separator character: '$arraySeparator'. Must be a single character.") }

  val quoteChar: Option[Char] =
    if (quote.isEmpty) { None }
    else if (quote.length == 1) { Some(quote.head) }
    else { throw new IllegalArgumentException(s"Invalid quote character: '$quote'. Must be a single character.") }

  val quoteEscapeChar: Char =
    if (quoteEscapeCharacter.length == 1) { quoteEscapeCharacter.head }
    else { throw new IllegalArgumentException(s"Invalid quote escape character: '$quoteEscapeCharacter'. Must be a single character.")}

  val codec: Codec = Codec(charset)

  protected val csvSettings: CsvSettings = CsvSettings(separatorChar, arraySeparatorChar, quoteChar,
    maxCharsPerColumn = Some(maxCharsPerColumn), quoteEscapeChar = quoteEscapeChar)
}
