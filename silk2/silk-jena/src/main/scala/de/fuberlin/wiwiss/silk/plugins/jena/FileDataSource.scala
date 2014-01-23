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
import de.fuberlin.wiwiss.silk.datasource.{ResourceLoader, DataSource}
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.{EntityDescription, Path, SparqlRestriction}
import de.fuberlin.wiwiss.silk.util.sparql.{EntityRetriever, SparqlAggregatePathsCollector}
import java.io.{OutputStreamWriter, File}
import org.apache.log4j.{Logger, PatternLayout, ConsoleAppender}
import com.hp.hpl.jena.query.QueryExecutionFactory

@Plugin(
  id = "file",
  label = "RDF dump",
  description =
    """ DataSource which retrieves all entities from an RDF file.
      | Parameters:
      |  file: File name inside {user.dir}/.silk/datasets/ or absolute path.
      |  format: Supported formats are: "RDF/XML", "N-Triples", "N-Quads", "Turtle"
      |  graph: The graph name to be read. If not provided, the default graph will be used.
      |         Must be provided if the format is N-Quads.
    """
)
case class FileDataSource(file: String, format: String, graph: String = "") extends DataSource {

  // Locate the file
  private val filePath = if(new File(file).isAbsolute) file else System.getProperty("user.home") + "/.silk/datasets/" + file

  // Try to parse the format
  private val lang = RDFLanguages.nameToLang(format)
  require(lang != null, "Supported formats are: \"RDF/XML\", \"N-Triples\", \"N-Quads\", \"Turtle\"")

  // Load dataset
  private var endpoint: JenaSparqlEndpoint = null

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String], resourceLoader: ResourceLoader) = {
    load()
    EntityRetriever(endpoint).retrieve(entityDesc, entities)
  }

  override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int], resourceLoader: ResourceLoader): Traversable[(Path, Double)] = {
    load()
    SparqlAggregatePathsCollector(endpoint, restrictions, limit)
  }

  /**
   * Loads the dataset and creates an endpoint.
   * Does nothing if the data set has already been loaded.
   */
  private def load() = synchronized {
    if(endpoint == null) {
      val dataset = RDFDataMgr.loadDataset(filePath, lang)

      val model =
        if(!graph.trim.isEmpty) dataset.getNamedModel(graph)
        else dataset.getDefaultModel

      endpoint = new JenaSparqlEndpoint(model)
    }
  }
}