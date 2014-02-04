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

import org.apache.jena.riot.{RDFLanguages, RDFDataMgr}
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.{EntityDescription, Path, SparqlRestriction}
import de.fuberlin.wiwiss.silk.util.sparql.{SparqlTypesCollector, EntityRetriever, SparqlAggregatePathsCollector}
import com.hp.hpl.jena.query.DatasetFactory
import de.fuberlin.wiwiss.silk.runtime.resource.Resource

@Plugin(
  id = "file",
  label = "RDF dump",
  description =
    """ DataSource which retrieves all entities from an RDF file.
      | Parameters:
      |  file: File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.
      |  format: Supported formats are: "RDF/XML", "N-Triples", "N-Quads", "Turtle"
      |  graph: The graph name to be read. If not provided, the default graph will be used.
      |         Must be provided if the format is N-Quads.
    """
)
case class FileDataSource(file: Resource, format: String, graph: String = "") extends DataSource {

  // Try to parse the format
  private val lang = RDFLanguages.nameToLang(format)
  require(lang != null, "Supported formats are: \"RDF/XML\", \"N-Triples\", \"N-Quads\", \"Turtle\"")

  // Load dataset
  private var endpoint: JenaSparqlEndpoint = null

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String]) = {
    load()
    EntityRetriever(endpoint).retrieve(entityDesc, entities)
  }

  override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    load()
    SparqlAggregatePathsCollector(endpoint, restrictions, limit)
  }

  override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
    load()
    SparqlTypesCollector(endpoint, limit)
  }

  /**
   * Loads the dataset and creates an endpoint.
   * Does nothing if the data set has already been loaded.
   */
  private def load() = synchronized {
    if(endpoint == null) {
      // Load data set
      val dataset = DatasetFactory.createMem()
      val inputStream = file.load
      RDFDataMgr.read(dataset, inputStream, lang)
      inputStream.close()

      // Retrieve model
      val model =
        if(!graph.trim.isEmpty) dataset.getNamedModel(graph)
        else dataset.getDefaultModel

      endpoint = new JenaSparqlEndpoint(model)
    }
  }
}