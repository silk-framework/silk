package org.silkframework.plugins.dataset.rdf.vocab

import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaDatasetEndpoint
import org.silkframework.rule.vocab.{Vocabulary, VocabularyManager}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.{FileResource, Resource}
import org.silkframework.util.Identifier

import java.io.File

@Plugin(
  id = "vocabularyFiles",
  label = "RDF Files",
  description = "Loads vocabularies from a directory of RDF files. At the moment, vocabularies are loaded once and cached. If the files are changed, the vocabularies need to be reloaded by restarting the application."
)
case class RdfFilesVocabularyManager(dir: String) extends VocabularyManager {

  /**
   * Holds all vocabularies by their URI.
   */
  private lazy val vocabularies: Map[String, Vocabulary] = {
    implicit val user: UserContext = UserContext.Empty
    for {
      file <- new File(dir).listFiles()
      vocabulary <- RdfFilesVocabularyManager.loadVocabulary(FileResource(file), file.toPath.toUri.toString)
    } yield {
      (vocabulary.info.uri, vocabulary)
    }
  }.toMap

  override def get(uri: String, project: Option[Identifier])
                  (implicit userContext: UserContext): Option[Vocabulary] = {
    vocabularies.get(uri)
  }

  override def retrieveGlobalVocabularies()(implicit userContext: UserContext): Option[Iterable[String]] = {
    Some(vocabularies.keys)
  }
}

object RdfFilesVocabularyManager {

  /**
   * Loads a vocabulary from a file.
   */
  def loadVocabulary(resource: Resource, uri: String)
                    (implicit userContext: UserContext): Option[Vocabulary] = {
    // Load into a Jena model
    val model = ModelFactory.createDefaultModel()
    val inputStream = resource.inputStream
    RDFDataMgr.read(model, inputStream, RDFLanguages.filenameToLang(resource.name))
    inputStream.close()

    // Create vocabulary loader
    val dataset = DatasetFactory.createTxnMem()
    dataset.addNamedModel(uri, model)
    val endpoint = new JenaDatasetEndpoint(dataset)
    val loader = new VocabularyLoader(endpoint)

    // Load vocabulary
    loader.retrieveVocabulary(uri)
  }

}
