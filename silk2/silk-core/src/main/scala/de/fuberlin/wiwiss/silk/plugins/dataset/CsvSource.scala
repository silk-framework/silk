package de.fuberlin.wiwiss.silk.plugins.dataset

import de.fuberlin.wiwiss.silk.dataset.DataSource
import de.fuberlin.wiwiss.silk.entity._
import de.fuberlin.wiwiss.silk.runtime.resource.Resource

import scala.io.Source

case class CsvSource(file: Resource, properties: String, separator: String = ",", prefix: String = "", uri: String = "", regexFilter: String = "") extends DataSource {

  private val propertyList: Seq[String] = properties.split(',')

  override def retrievePaths(restriction: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    for(property <- propertyList) yield {
      (Path.parse("?" + restriction.variable + "/<" + prefix + property + ">"), 1.0)
    }
  }

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String] = Seq.empty): Traversable[Entity] = {
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
        val inputStream = file.load
        val source = Source.fromInputStream(inputStream)
        try {
          // Iterate through all lines of the source file. If a *regexFilter* has been set, then use it to filter
          // the rows.
          for {
              (line, number) <- source.getLines.zipWithIndex
                .filter(regexFilter.isEmpty || _._1.matches(regexFilter))
          } {

            //Split the line into values
            val allValues = line.split(separator)
            assert(propertyList.size == allValues.size, "Invalid line '" + line + "' with " + allValues.size + " elements. Expected numer of elements " + propertyList.size + ".")
            //Extract requested values
            val values = indices.map(allValues(_))

            // The default URI pattern is to use the prefix and the line number.
            // However the user can specify a different URI pattern (in the *uri* property), which is then used to
            // build the entity URI. An example of such pattern is 'urn:zyx:{id}' where *id* is a name of a property
            // as defined in the *properties* field.
            val entityURI = if (uri.isEmpty)
              prefix + number
            else
              "\\{([^\\}]+)\\}".r.replaceAllIn(uri, m => {
                val propName = m.group(1)

                assert(propertyList.contains(propName))
                allValues(propertyList.indexOf(propName))
              })


            //Build entity
            f(new Entity(
              uri = entityURI,
              values = values.map(Set(_)),
              desc = entityDesc
            ))

          }
        } finally {
          source.close()
          inputStream.close()
        }
      }
    }
  }
}
