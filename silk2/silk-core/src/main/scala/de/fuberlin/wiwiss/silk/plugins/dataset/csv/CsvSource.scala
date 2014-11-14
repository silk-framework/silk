package de.fuberlin.wiwiss.silk.plugins.dataset.csv

import java.util.logging.{Level, Logger}
import java.util.regex.Pattern

import de.fuberlin.wiwiss.silk.dataset.DataSource
import de.fuberlin.wiwiss.silk.entity._
import de.fuberlin.wiwiss.silk.runtime.resource.Resource

import scala.io.{Codec, Source}

class CsvSource(file: Resource, properties: String, separator: String, arraySeparator: String, prefix: String = "", uri: String = "", regexFilter: String = "") extends DataSource {

  private val logger = Logger.getLogger(getClass.getName)

  private lazy val propertyList: Seq[String] = {
    if (!properties.trim.isEmpty)
      properties.split(',')
    else {
      val source = Source.fromInputStream(file.load)
      val firstLine = source.getLines().next()
      source.close()
      firstLine.split(separator)
    }
  }

  override def retrievePaths(restriction: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    for (property <- propertyList) yield {
      (Path(restriction.variable, ForwardOperator(prefix + property) :: Nil), 1.0)
    }
  }

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String] = Seq.empty): Traversable[Entity] = {

    logger.log(Level.FINE, "Retrieving data from CSV.")

    // Retrieve the indices of the request paths
    val indices =
      for (path <- entityDesc.paths) yield {
        val property = path.operators.head.asInstanceOf[ForwardOperator].property.uri.stripPrefix(prefix)
        val propertyIndex = propertyList.indexOf(property)
        if (propertyIndex == -1)
          throw new Exception("Property " + path.toString + " not found in CSV")
        propertyIndex
      }

    // Return new Traversable that generates an entity for each line
    new Traversable[Entity] {
      def foreach[U](f: Entity => U) {
        val inputStream = file.load
        val source = Source.fromInputStream(inputStream)(Codec.UTF8)

        // Compile the line regex.
        val regex: Pattern = if (!regexFilter.isEmpty) regexFilter.r.pattern else null

        try {
          // Iterate through all lines of the source file. If a *regexFilter* has been set, then use it to filter
          // the rows.

          source.getLines().zipWithIndex
            .withFilter(l => !(properties.trim.isEmpty && 0 == l._2) && (regexFilter.isEmpty || regex.matcher(l._1).matches()))
            .foreach {

            case (line, number) =>

              logger.log(Level.FINER, s"Retrieving data from CSV [ line number :: ${number + 1} ].")

              //Split the line into values
              val allValues = line.split(separator, -1)
              assert(propertyList.size == allValues.size, s"Invalid line ${number + 1}: '$line' in resource '${file.name}' with ${allValues.size} elements. Expected number of elements ${propertyList.size}.")
              //Extract requested values
              val values = indices.map(allValues(_))

              // The default URI pattern is to use the prefix and the line number.
              // However the user can specify a different URI pattern (in the *uri* property), which is then used to
              // build the entity URI. An example of such pattern is 'urn:zyx:{id}' where *id* is a name of a property
              // as defined in the *properties* field.
              val entityURI = if (uri.isEmpty)
                prefix + (number + 1)
              else
                "\\{([^\\}]+)\\}".r.replaceAllIn(uri, m => {
                  val propName = m.group(1)

                  assert(propertyList.contains(propName))
                  allValues(propertyList.indexOf(propName))
                })


              //Build entity
              if (entities.isEmpty || entities.contains(entityURI)) {
                val entityValues =
                  if (arraySeparator.isEmpty)
                    values.map(Set(_))
                  else
                    values.map(_.split(arraySeparator, -1).toSet)

                f(new Entity(
                  uri = entityURI,
                  values = entityValues,
                  desc = entityDesc
                ))
              }
          }

        } finally {
          source.close()
          inputStream.close()
        }
      }
    }
  }
}
