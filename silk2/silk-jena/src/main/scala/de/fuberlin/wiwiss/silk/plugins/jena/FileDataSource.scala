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

import com.hp.hpl.jena.rdf.model.ModelFactory
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.{EntityDescription, Path, SparqlRestriction}
import de.fuberlin.wiwiss.silk.util.sparql.{EntityRetriever, SparqlAggregatePathsCollector}
import java.io.{File, FileInputStream}

/**
 * DataSource which retrieves all instances from an RDF file.
 *
 * Parameters:
 * - '''file''': The RDF file
 * - '''format''': The format of the RDF file. Allowed values: "RDF/XML", "N-TRIPLE", "TURTLE", "TTL", "N3"
 */
@Plugin(id = "file", label = "RDF dump", description = "DataSource which retrieves all entities from an RDF file. By default the dumps are read from {user.dir}/.silk/datasets/. Suported formats are: \"RDF/XML\", \"N-TRIPLE\", \"TURTLE\", \"TTL\", \"N3\"")
case class FileDataSource(file: String, format: String) extends DataSource {
  private val filePath = if(new File(file).isAbsolute) file else System.getProperty("user.home") + "/.silk/datasets/" + file

  private lazy val model = ModelFactory.createDefaultModel
  model.read(new FileInputStream(filePath), null, format)

  private lazy val endpoint = new JenaSparqlEndpoint(model)

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String]) = {
    EntityRetriever(endpoint).retrieve(entityDesc, entities)
  }

  override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    SparqlAggregatePathsCollector(endpoint, restrictions, limit)
  }
}
