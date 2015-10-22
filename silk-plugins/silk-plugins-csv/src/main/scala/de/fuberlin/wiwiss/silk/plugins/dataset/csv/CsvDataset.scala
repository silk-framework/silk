package de.fuberlin.wiwiss.silk.plugins.dataset.csv

import de.fuberlin.wiwiss.silk.dataset.{DataSink, DataSource, DatasetPlugin}
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.runtime.resource.Resource

import scala.io.Codec

@Plugin(
  id = "csv",
  label = "CSV",
  description =
      """Retrieves all entities from a csv file.
Parameters:
  file:  File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.
  properties: Comma-separated list of properties. If not provided, the list of properties is read from the first line.
  separator: The character that is used to separate values.  If not provided, defaults to ',', i.e., comma-separated values.
             '\t' for specifying tab-separated values, is also supported.
  arraySeparator: The character that is used to separate the parts of array values.
  prefix: A URI prefix that should be used for generating schema entities like classes or properties, e.g. http://www4.wiwiss.fu-berlin.de/ontology/
  uri: A pattern used to construct the entity URI. If not provided the prefix + the line number is used.
       An example of such a pattern is 'urn:zyx:{id}' where *id* is a name of a property.
  regexFilter: A regex filter used to match rows from the CSV file. If not set all the rows are used.
  charset: The file encoding, e.g., UTF8, ISO-8859-1"""
)
case class CsvDataset(file: Resource, properties: String = "", separator: String = ",", arraySeparator: String = "", quote: String = "",
                      prefix: String = "", uri: String = "", regexFilter: String = "", charset: String = "UTF8") extends DatasetPlugin {

  private val sepChar =
    if(separator == "\\t") '\t'
    else if(separator.length == 1) separator.head
    else throw new IllegalArgumentException(s"Invalid separator: '$separator'. Must be a single character.")

  private val arraySeparatorChar =
    if(arraySeparator.isEmpty) None
    else if(arraySeparator.length == 1) Some(arraySeparator.head)
    else throw new IllegalArgumentException(s"Invalid array separator character: '$arraySeparator'. Must be a single character.")

  private val quoteChar =
    if(quote.isEmpty) None
    else if(quote.length == 1) Some(quote.head)
    else throw new IllegalArgumentException(s"Invalid quote character: '$quote'. Must be a single character.")

  private val codec = Codec(charset)

  private val settings = CsvSettings(sepChar, arraySeparatorChar, quoteChar)

  override def source: DataSource = new CsvSource(file, settings, properties, prefix, uri, regexFilter, codec)

  override def sink: DataSink = new CsvSink(file, settings)
}
