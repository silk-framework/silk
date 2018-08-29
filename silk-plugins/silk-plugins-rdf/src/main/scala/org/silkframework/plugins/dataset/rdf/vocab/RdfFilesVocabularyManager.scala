package org.silkframework.plugins.dataset.rdf.vocab

import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaDatasetEndpoint
import org.silkframework.rule.vocab.{Vocabulary, VocabularyManager}
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.Identifier
import org.silkframework.workspace.WorkspaceFactory

@Plugin(
  id = "rdfFiles",
  label = "RDF Files",
  description = "Loads vocabularies from RDF files, which are part of the project resources."
)
case class RdfFilesVocabularyManager() extends VocabularyManager {

  private val prefix = "urn:"

  override def get(uri: String, project: Identifier): Vocabulary = {
    // Get resource
    val vocabularyResource = WorkspaceFactory().workspace.project(project).resources.get(uri)

    // Load into Jena model
    val model = ModelFactory.createDefaultModel()
    val inputStream = vocabularyResource.inputStream
    RDFDataMgr.read(model, inputStream, RDFLanguages.filenameToLang(vocabularyResource.name))
    inputStream.close()

    // Create vocabulary loader
    val dataset = DatasetFactory.createTxnMem()
    dataset.addNamedModel(prefix + uri, model)
    val endpoint = new JenaDatasetEndpoint(dataset)
    val loader = new VocabularyLoader(endpoint)

    // Load vocabulary
    loader.retrieveVocabulary(prefix + uri)
  }
}
