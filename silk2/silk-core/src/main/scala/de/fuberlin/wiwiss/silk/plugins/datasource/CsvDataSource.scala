package de.fuberlin.wiwiss.silk.plugins.datasource

import de.fuberlin.wiwiss.silk.datasource.{ResourceLoader, DataSource}
import de.fuberlin.wiwiss.silk.entity._
import io.Source
import java.io.File
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(
  id = "csv",
  label = "CSV Source",
  description = "DataSource which retrieves all entities from a csv file.")
case class CsvDataSource(url: String, properties: String, separator: Char = ',', prefix: String = "") extends DataSource {

  private val propertyList: Seq[String] = properties.split(separator)

  override def retrievePaths(restriction: SparqlRestriction, depth: Int, limit: Option[Int], resourceLoader: ResourceLoader): Traversable[(Path, Double)] = {
    for(property <- propertyList) yield {
      (Path.parse("?" + restriction.variable + "/<" + prefix + property + ">"), 1.0)
    }
  }

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String] = Seq.empty, resourceLoader: ResourceLoader): Traversable[Entity] = {
    // Retrieve the indices of the request paths
    val indices =
      for(path <- entityDesc.paths) yield {
        val property = path.operators.head.asInstanceOf[ForwardOperator].property.uri.stripPrefix(prefix)
        val propertyIndex = propertyList.indexOf(property)
        propertyIndex
      }

    // Return new Traversable that generates an entity for each line
    new Traversable[Entity] {
      def foreach[U](f: Entity => U) {
        val source = openSource()
        try {
          // Iterate through all lines of the source file.
          for ((line, number) <- source.getLines.zipWithIndex) {
            //Split the line into values
            val allValues = line.split(separator)
            assert(propertyList.size == allValues.size, "Invalid line '" + line + "' with " + allValues.size + " elements. Expected numer of elements " + propertyList.size + ".")
            //Extract requested values
            val values = indices.map(allValues(_))
            //Build entity
            f(new Entity(
              uri = prefix + number,
              values = values.map(Set(_)),
              desc = entityDesc
            ))
          }
        } finally {
          source.close()
        }
      }
    }
  }

  /**
   * Creates scala.io.Source for the given URL.
   * Supports the classpath: protocol, which is not supported by default in scala.io.Source.
   */
  private def openSource(): Source = {
    if(url.startsWith("classpath:"))
      Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(url.stripPrefix("classpath:")))
    else
      Source.fromURL(url)
  }
}
