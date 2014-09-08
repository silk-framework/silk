package de.fuberlin.wiwiss.silk.plugins.dataset.csv

import de.fuberlin.wiwiss.silk.dataset.{DataSink, DataSource, DatasetPlugin}
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.runtime.resource.Resource

@Plugin(
  id = "csv",
  label = "CSV",
  description =
      """Retrieves all entities from a csv file.
Parameters:
  file:  File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.
  properties: Comma-separated list of properties. If not provided, the list of properties is read from the first line.
  separator: The character that is used to separate values.  If not provided, defaults to ',', i.e., comma-separated values.
             Regexes, such as '\t' for specifying tab-separated values, are also supported.
  prefix: The prefix that is used to generate URIs for each line.
  uri: A pattern used to construct the entity URI. If not provided the prefix + the line number is used.
       An example of such a pattern is 'urn:zyx:{id}' where *id* is a name of a property.
  regexFilter: A regex filter used to match rows from the CSV file. If not set all the rows are used."""
)
case class CsvDataset(file: Resource, properties: String, separator: String = ",", prefix: String = "", uri: String = "", regexFilter: String = "") extends DatasetPlugin {

  override def source: DataSource = new CsvSource(file, properties, separator, prefix, uri, regexFilter)

  override def sink: DataSink = throw new NotImplementedError("CSVs cannot be written at the moment")
}
