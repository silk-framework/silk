/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.plugins.jena

import de.fuberlin.wiwiss.silk.datasource.DataSource
import com.hp.hpl.jena.rdf.model.ModelFactory
import java.io.StringReader
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.sparql.EntityRetriever
import de.fuberlin.wiwiss.silk.entity.{Path, SparqlRestriction, EntityDescription, Entity}
import de.fuberlin.wiwiss.silk.util.sparql.SparqlAggregatePathsCollector

/**
 * A DataSource where all entities are given directly in the configuration.
 *
 * Parameters:
 * - '''input''': The input data
 * - '''format''': The format of the input data. Allowed values: "RDF/XML", "N-TRIPLE", "TURTLE", "TTL", "N3"
 */
@Plugin(id = "rdf", label = "RDF", description = "A DataSource where all entities are given directly in the configuration.")
case class RdfDataSource(input: String, format: String) extends DataSource {
  private lazy val model = ModelFactory.createDefaultModel
  model.read(new StringReader(input), null, format)

  private lazy val endpoint = new JenaSparqlEndpoint(model)

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String]): Traversable[Entity] = {
    EntityRetriever(endpoint).retrieve(entityDesc, entities)
  }

  override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    SparqlAggregatePathsCollector(endpoint, restrictions, limit)
  }
}
