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

package de.fuberlin.wiwiss.silk.plugins.jena

import de.fuberlin.wiwiss.silk.datasource.DataSource
import com.hp.hpl.jena.rdf.model.Model
import de.fuberlin.wiwiss.silk.entity.{EntityDescription, Path, SparqlRestriction}
import de.fuberlin.wiwiss.silk.util.sparql.{SparqlEndpoint, EntityRetriever, SparqlAggregatePathsCollector}

/**
 * DataSource which retrieves all instances from an RDF file.
 *
 * Parameters:
 * - '''file''': The RDF file
 * - '''format''': The format of the RDF file. Allowed values: "RDF/XML", "N-TRIPLE", "TURTLE", "TTL", "N3"
 */

class ModelDataSource() extends DataSource {

  private var endpoint: Option[SparqlEndpoint] = None

  def setModel(model: Model) = {
    endpoint = Some(new JenaSparqlEndpoint(model))
  }


  override def retrieve(entityDesc: EntityDescription, instances: Seq[String]) = {
    if (endpoint.isEmpty){
      throw new IllegalStateException("Model has not yet been specified through setModel()")
    }
    EntityRetriever(endpoint.get).retrieve(entityDesc, instances)
  }

  override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    if (endpoint.isEmpty){
      throw new IllegalStateException("Model has not yet been specified through setModel()")
    }
    SparqlAggregatePathsCollector(endpoint.get, restrictions, limit)
  }
}
