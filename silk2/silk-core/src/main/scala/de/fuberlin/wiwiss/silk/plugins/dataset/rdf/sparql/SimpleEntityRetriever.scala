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

package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.sparql

import de.fuberlin.wiwiss.silk.dataset.rdf.{Resource, RdfNode, SparqlEndpoint}
import de.fuberlin.wiwiss.silk.entity.{Entity, EntityDescription, Path}

/**
 * EntityRetriever which executes a single SPARQL query to retrieve the entities.
 */
class SimpleEntityRetriever(endpoint: SparqlEndpoint, pageSize: Int = 1000, graphUri: Option[String] = None) extends EntityRetriever {
  private val varPrefix = "v"

  /**
   * Retrieves entities with a given entity description.
   *
   * @param entityDesc The entity description
   * @param entities The URIs of the entities to be retrieved. If empty, all entities will be retrieved.
   * @return The retrieved entities
   */
  override def retrieve(entityDesc: EntityDescription, entities: Seq[String]): Traversable[Entity] = {
    if (entities.isEmpty) {
      retrieveAll(entityDesc)
    } else {
      retrieveList(entities, entityDesc)
    }
  }

  /**
   * Retrieves all entities with a given entity description.
   *
   * @param entityDesc The entity description
   * @return The retrieved entities
   */
  private def retrieveAll(entityDesc: EntityDescription): Traversable[Entity] = {
    //Select
    var sparql = "SELECT DISTINCT "
    sparql += "?" + entityDesc.variable + " "
    for (i <- 0 until entityDesc.paths.size) {
      sparql += "?" + varPrefix + i + " "
    }
    sparql += "\n"

    //Graph
    for (graph <- graphUri if !graph.isEmpty) sparql += "FROM <" + graph + ">\n"

    //Body
    sparql += "WHERE {\n"
    if (entityDesc.restrictions.toSparql.isEmpty)
      sparql += "?" + entityDesc.variable + " ?" + varPrefix + "_p ?" + varPrefix + "_o .\n"
    else
      sparql += entityDesc.restrictions.toSparql + "\n"
    sparql += SparqlPathBuilder(entityDesc.paths, "?" + entityDesc.variable, "?" + varPrefix)
    sparql += "}"

    val sparqlResults = endpoint.query(sparql)

    new EntityTraversable(sparqlResults.bindings, entityDesc, None)
  }

  /**
   * Retrieves a list of entities.
   *
   * @param entityUris The URIs of the entities
   * @param entityDesc The entity description
   * @return A sequence of the retrieved entities.
   */
  private def retrieveList(entityUris: Seq[String], entityDesc: EntityDescription): Seq[Entity] = {
    entityUris.view.flatMap(entityUri => retrieveEntity(entityUri, entityDesc))
  }

  /**
   * Retrieves a single entity.
   *
   * @param entityUri The URI of the entity
   * @param entityDesc The entity description
   * @return Some(entity), if an entity with the given uri is in the Store
   *         None, if no entity with the given uri is in the Store
   */
  def retrieveEntity(entityUri: String, entityDesc: EntityDescription): Option[Entity] = {
    //Query only one path at once and combine the result into one
    val sparqlResults = {
      for ((path, pathIndex) <- entityDesc.paths.zipWithIndex;
           results <- retrievePaths(entityDesc, entityUri, Seq(path))) yield {
        results map {
          case (variable, node) => (varPrefix + pathIndex, node)
        }
      }
    }

    new EntityTraversable(sparqlResults, entityDesc, Some(entityUri)).headOption
  }

  private def retrievePaths(entityDesc: EntityDescription, entityUri: String, paths: Seq[Path]) = {
    //Select
    var sparql = "SELECT DISTINCT "
    for (i <- 0 until paths.size) {
      sparql += "?" + varPrefix + i + " "
    }
    sparql += "\n"

    //Graph
    for (graph <- graphUri) sparql += "FROM <" + graph + ">\n"

    //Body
    sparql += "WHERE {\n"
    sparql += SparqlPathBuilder(paths, "<" + entityUri + ">", "?" + varPrefix)
    sparql += "}"

    endpoint.query(sparql).bindings
  }

  /**
   * Wraps a Traversable of SPARQL results and retrieves entities from them.
   */
  private class EntityTraversable(sparqlResults: Traversable[Map[String, RdfNode]], entityDesc: EntityDescription, subject: Option[String]) extends Traversable[Entity] {
    override def foreach[U](f: Entity => U) {
      //Remember current subject
      var curSubject: Option[String] = subject

      //Collect values of the current subject
      var values = Array.fill(entityDesc.paths.size)(Set[String]())

      for (result <- sparqlResults) {
        //If the subject is unknown, find binding for subject variable
        if (subject.isEmpty) {
          //Check if we are still reading values for the current subject
          val resultSubject = result.get(entityDesc.variable) match {
            case Some(Resource(value)) => Some(value)
            case _ => None
          }

          if (resultSubject != curSubject) {
            for (curSubjectUri <- curSubject) {
              f(new Entity(curSubjectUri, values, entityDesc))
            }

            curSubject = resultSubject
            values = Array.fill(entityDesc.paths.size)(Set[String]())
          }
        }

        //Find results for values for the current subject
        if (curSubject.isDefined) {
          for ((variable, node) <- result if variable.startsWith(varPrefix)) {
            val id = variable.substring(varPrefix.length).toInt

            values(id) += node.value
          }
        }
      }

      for (curSubjectUri <- curSubject) {
        f(new Entity(curSubjectUri, values, entityDesc))
      }
    }
  }

}