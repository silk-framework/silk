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
import java.io.{FileInputStream, OutputStreamWriter, File}
import org.apache.log4j.{Logger, PatternLayout, ConsoleAppender}
import com.hp.hpl.jena.query.{DatasetFactory, QueryExecutionFactory}

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
case class FileDataSource(file: String, format: String, graph: String = "") extends DataSource {

  // Try to parse the format
  private val lang = RDFLanguages.nameToLang(format)
  require(lang != null, "Supported formats are: \"RDF/XML\", \"N-Triples\", \"N-Quads\", \"Turtle\"")

  // Load dataset
  private var endpoint: JenaSparqlEndpoint = null

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String], resourceLoader: ResourceLoader) = {
    load(resourceLoader)
    EntityRetriever(endpoint).retrieve(entityDesc, entities)
  }

  override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int], resourceLoader: ResourceLoader): Traversable[(Path, Double)] = {
    load(resourceLoader)
    SparqlAggregatePathsCollector(endpoint, restrictions, limit)
  }

  /**
   * Loads the dataset and creates an endpoint.
   * Does nothing if the data set has already been loaded.
   */
  private def load(resourceLoader: ResourceLoader) = synchronized {
    if(endpoint == null) {
      // We still need to support the old method of putting files in a dataset directory in the user home
      val oldFileLocation =
        if(new File(file).isAbsolute) new File(file)
        else new File(System.getProperty("user.home") + "/.silk/datasets/" + file)

      val inputStream = {
        if(oldFileLocation.exists()) new FileInputStream(oldFileLocation)
        else resourceLoader.load(file)
      }

      // Load data set
      val dataset = DatasetFactory.createMem()
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