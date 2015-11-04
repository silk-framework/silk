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

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.dataset.rdf.SparqlEndpoint
import de.fuberlin.wiwiss.silk.entity.rdf.SparqlRestriction
import de.fuberlin.wiwiss.silk.entity.{ForwardOperator, Path}
import de.fuberlin.wiwiss.silk.util.Uri

/**
 * Retrieves the most frequent paths of a number of random sample entities.
 *
 * It is typically faster than SparqlAggregatePathsCollector but also less precise.
 *
 * The limitations of the current implementation are:
 * - It does only return forward paths of length 1
 * - It returns a maximum of 100 paths
 */
object SparqlSamplePathsCollector extends SparqlPathsCollector {
  /**Number of sample entities */
  private val maxEntities = 100

  /**The minimum frequency of a property to be considered relevant */
  private val MinFrequency = 0.01

  private implicit val logger = Logger.getLogger(SparqlSamplePathsCollector.getClass.getName)

  def apply(endpoint: SparqlEndpoint, restrictions: SparqlRestriction, limit: Option[Int]): Seq[Path] = {
    val sampleEntities = {
      if (restrictions.isEmpty)
        getAllEntities(endpoint)
      else
        getEntities(endpoint, restrictions)
    }

    getEntitiesPaths(endpoint, sampleEntities, restrictions.variable, limit.getOrElse(100))
  }

  private def getAllEntities(endpoint: SparqlEndpoint): Traversable[String] = {
    val sparql = "SELECT ?s WHERE { ?s ?p ?o }"

    val results = endpoint.select(sparql, maxEntities)

    results.bindings.map(_("s").value)
  }

  private def getEntities(endpoint: SparqlEndpoint, restrictions: SparqlRestriction): Traversable[String] = {
    val sparql = "SELECT ?" + restrictions.variable + " WHERE { " + restrictions.toSparql + " }"

    val results = endpoint.select(sparql, maxEntities)

    results.bindings.map(_(restrictions.variable).value)
  }

  private def getEntitiesPaths(endpoint: SparqlEndpoint, entities: Traversable[String], variable: String, limit: Int): Seq[Path] = {
    logger.info("Searching for relevant properties in " + endpoint)

    val entityArray = entities.toArray

    //Get all properties
    val properties = entityArray.flatMap(entity => getEntityProperties(endpoint, entity, variable, limit))

    //Compute the frequency of each property
    val propertyFrequencies = properties.groupBy(x => x).mapValues(_.size.toDouble / entityArray.size).toList

    //Choose the relevant properties
    val relevantProperties = propertyFrequencies.filter { case (uri, frequency) => frequency > MinFrequency }
                                                .sortWith(_._2 > _._2).take(limit).map(_._1)

    logger.info("Found " + relevantProperties.size + " relevant properties in " + endpoint)

    relevantProperties
  }

  private def getEntityProperties(endpoint: SparqlEndpoint, entityUri: String, variable: String, limit: Int): Traversable[Path] = {
    var sparql = ""
    sparql += "SELECT DISTINCT ?p \n"
    sparql += "WHERE {\n"
    sparql += " <" + entityUri + "> ?p ?o\n"
    sparql += "}"

    for (result <- endpoint.select(sparql, limit).bindings; binding <- result.values) yield
      Path(variable, ForwardOperator(Uri.fromURI(binding.value)) :: Nil)
  }
}