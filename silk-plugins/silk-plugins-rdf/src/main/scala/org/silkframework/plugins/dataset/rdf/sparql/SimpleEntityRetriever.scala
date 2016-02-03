/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.plugins.dataset.rdf.sparql

import org.silkframework.dataset.rdf.{Resource, RdfNode, SparqlEndpoint}
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.entity.{EntitySchema, Entity, Path}
import org.silkframework.util.Uri

/**
 * EntityRetriever which executes a single SPARQL query to retrieve the entities.
 */
class SimpleEntityRetriever(endpoint: SparqlEndpoint, pageSize: Int = 1000, graphUri: Option[String] = None, useOrderBy: Boolean = true) extends EntityRetriever {
  private val varPrefix = "v"

  /**
   * Retrieves entities with a given entity description.
   *
   * @param entitySchema The entity description
   * @param entities The URIs of the entities to be retrieved. If empty, all entities will be retrieved.
   * @return The retrieved entities
   */
  override def retrieve(entitySchema: EntitySchema, entities: Seq[Uri], limit: Option[Int]): Traversable[Entity] = {
    if (entities.isEmpty) {
      retrieveAll(entitySchema, limit)
    } else {
      retrieveList(entities, entitySchema)
    }
  }

  /**
   * Retrieves all entities with a given entity description.
   *
   * @param entitySchema The entity schema
   * @return The retrieved entities
   */
  private def retrieveAll(entitySchema: EntitySchema, limit: Option[Int]): Traversable[Entity] = {
    val sparqlEntitySchema = SparqlEntitySchema.fromSchema(entitySchema)
    //Select
    var sparql = "SELECT DISTINCT "
    sparql += "?" + sparqlEntitySchema.variable + " "
    for (i <- sparqlEntitySchema.paths.indices) {
      sparql += "?" + varPrefix + i + " "
    }
    sparql += "\n"

    //Graph
    for (graph <- graphUri if !graph.isEmpty) sparql += "FROM <" + graph + ">\n"

    //Body
    sparql += "WHERE {\n"
    if (!sparqlEntitySchema.restrictions.toSparql.isEmpty)
      sparql += sparqlEntitySchema.restrictions.toSparql + "\n"
    else
      sparql += "?" + sparqlEntitySchema.variable + " ?" + varPrefix + "_p ?" + varPrefix + "_o .\n"

    sparql += SparqlPathBuilder(sparqlEntitySchema.paths, "?" + sparqlEntitySchema.variable, "?" + varPrefix)
    sparql += "}"
    if(useOrderBy) sparql +=" ORDER BY ?" + sparqlEntitySchema.variable

    val sparqlResults = endpoint.select(sparql)

    new EntityTraversable(sparqlResults.bindings, entitySchema, None, limit)
  }

  /**
   * Retrieves a list of entities.
   *
   * @param entityUris The URIs of the entities
   * @param entitySchema The entity description
   * @return A sequence of the retrieved entities.
   */
  private def retrieveList(entityUris: Seq[Uri], entitySchema: EntitySchema): Seq[Entity] = {
    entityUris.view.flatMap(entityUri => retrieveEntity(entityUri, entitySchema))
  }

  /**
   * Retrieves a single entity.
   *
   * @param entityUri The URI of the entity
   * @param entitySchema The entity description
   * @return Some(entity), if an entity with the given uri is in the Store
   *         None, if no entity with the given uri is in the Store
   */
  def retrieveEntity(entityUri: Uri, entitySchema: EntitySchema): Option[Entity] = {
    //Query only one path at once and combine the result into one
    val sparqlResults = {
      for ((path, pathIndex) <- entitySchema.paths.zipWithIndex;
           results <- retrievePaths(entitySchema, entityUri, Seq(path))) yield {
        results map {
          case (variable, node) => (varPrefix + pathIndex, node)
        }
      }
    }

    new EntityTraversable(sparqlResults, entitySchema, Some(entityUri), None).headOption
  }

  private def retrievePaths(entityDesc: EntitySchema, entityUri: Uri, paths: Seq[Path]) = {
    //Select
    var sparql = "SELECT DISTINCT "
    for (i <- paths.indices) {
      sparql += "?" + varPrefix + i + " "
    }
    sparql += "\n"

    //Graph
    for (graph <- graphUri) sparql += "FROM <" + graph + ">\n"

    //Body
    sparql += "WHERE {\n"
    sparql += SparqlPathBuilder(paths, "<" + entityUri + ">", "?" + varPrefix)
    sparql += "}"

    endpoint.select(sparql).bindings
  }

  /**
   * Wraps a Traversable of SPARQL results and retrieves entities from them.
   */
  private class EntityTraversable(sparqlResults: Traversable[Map[String, RdfNode]], entitySchema: EntitySchema, subject: Option[Uri], limit: Option[Int]) extends Traversable[Entity] {
    val sparqlEntitySchema = SparqlEntitySchema.fromSchema(entitySchema)
    override def foreach[U](f: Entity => U) {
      //Remember current subject
      var curSubject: Option[String] = subject.map(_.uri)

      //Collect values of the current subject
      var values = Array.fill(entitySchema.paths.size)(Seq[String]())

      // Count retrieved entities
      var counter = 0

      for (result <- sparqlResults) {
        //If the subject is unknown, find binding for subject variable
        if (subject.isEmpty) {
          //Check if we are still reading values for the current subject
          val resultSubject = result.get(sparqlEntitySchema.variable) match {
            case Some(Resource(value)) => Some(value)
            case _ => None
          }

          if (resultSubject != curSubject) {
            for (curSubjectUri <- curSubject) {
              f(new Entity(curSubjectUri, values, entitySchema))
              counter += 1
              if(limit.exists(counter >= _))
                return
            }

            curSubject = resultSubject
            values = Array.fill(entitySchema.paths.size)(Seq[String]())
          }
        }

        //Find results for values for the current subject
        if (curSubject.isDefined) {
          for ((variable, node) <- result if variable.startsWith(varPrefix)) {
            val id = variable.substring(varPrefix.length).toInt

            values(id) = values(id) :+ node.value
          }
        }
      }

      for (curSubjectUri <- curSubject) {
        f(new Entity(curSubjectUri, values, entitySchema))
      }
    }
  }

}