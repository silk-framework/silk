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

import de.fuberlin.wiwiss.silk.entity.{Entity, EntityDescription}
import com.hp.hpl.jena.rdf.model.ModelFactory
import de.fuberlin.wiwiss.silk.datasource.{ResourceLoader, DataSource}
import de.fuberlin.wiwiss.silk.util.sparql.EntityRetriever
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import org.apache.jena.riot.RDFDataMgr
import com.hp.hpl.jena.query.DatasetFactory

@Plugin(id = "linkedData", label = "Linked Data", description = "TODO")
case class LinkedDataSource() extends DataSource {
  override def retrieve(entityDesc: EntityDescription, entities: Seq[String], resourceLoader: ResourceLoader): Traversable[Entity] = {
    require(!entities.isEmpty, "Retrieving all entities not supported")

    val model = ModelFactory.createDefaultModel
    for (uri <- entities) {
      model.read(uri)
    }

    val endpoint = new JenaSparqlEndpoint(model)

    val entityRetriever = EntityRetriever(endpoint)

    entityRetriever.retrieve(entityDesc, entities)
  }
}