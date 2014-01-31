package de.fuberlin.wiwiss.silk.preprocessing.util.jena

import java.io.File
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import de.fuberlin.wiwiss.silk.preprocessing.util.sparql.SimpleRetriever


/**
 * Created by Petar on 27/01/14.
 */
case class JenaSource(file: String, format: String, graph: String = "") {
  // Locate the file
  private val filePath = if(new File(file).isAbsolute) file else System.getProperty("user.home") + "/.silk/datasets/" + file

  // Try to parse the format
  private val lang = RDFLanguages.nameToLang(format)
  require(lang != null, "Supported formats are: \"RDF/XML\", \"N-Triples\", \"N-Quads\", \"Turtle\"")

  // Load dataset
  private lazy val endpoint = load()

  def retrieve() = {
    SimpleRetriever(endpoint).retrieve()
  }



  /**
   * Loads the dataset and creates an endpoint.
   */
  private def load() = {
    val dataset = RDFDataMgr.loadDataset(filePath, lang)

    val model =
      if(!graph.trim.isEmpty) dataset.getNamedModel(graph)
      else dataset.getDefaultModel

    new SparqlEndpoint(model)
  }
}
