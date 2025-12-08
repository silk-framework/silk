package org.silkframework.plugins.dataset.rdf.vocab

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.apache.jena.vocabulary.OWL2
import org.silkframework.plugins.dataset.rdf.vocab.InMemoryVocabularyManager.vocabularies
import org.silkframework.rule.vocab.{Vocabulary, VocabularyManager}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.{FileResource, Resource}
import org.silkframework.util.Identifier

import java.io.File
import scala.jdk.CollectionConverters.IteratorHasAsScala

@Plugin(
  id = "inMemoryVocabularyManager",
  label = "In-Memory Vocabulary Manager",
  description = "Holds all vocabularies in memory. Mostly used for testing. Vocabularies can only be added programmically by calling the addVocabulary method.",
)
case class InMemoryVocabularyManager() extends VocabularyManager {

  override def get(uri: String, project: Option[Identifier])
                  (implicit userContext: UserContext): Option[Vocabulary] = {
    vocabularies.get(uri)
  }

  override def retrieveGlobalVocabularies()(implicit userContext: UserContext): Option[Iterable[String]] = {
    Some(vocabularies.keys)
  }
}

object InMemoryVocabularyManager {

  /**
   * Holds all vocabularies by their URI.
   */
  private var vocabularies: Map[String, Vocabulary] = Map.empty

  /**
   * Adds a vocabulary from a file to the in-memory vocabulary manager.
   *
   * @param file The file containing the vocabulary in RDF format.
   * @param userContext The user context for loading the vocabulary.
   */
  def addVocabulary(file: File)
                   (implicit userContext: UserContext): Unit = {
    val resource = FileResource(file)
    val vocabulary = RdfFilesVocabularyManager.loadVocabulary(resource, vocabularyUri(resource))
      .getOrElse(throw new IllegalArgumentException(s"Could not load vocabulary from file: ${file.getAbsolutePath}"))
    vocabularies += (vocabulary.info.uri -> vocabulary)
  }

  /**
   * Retrieve the vocabulary URI from a vocabulary file.
   */
  private def vocabularyUri(resource: Resource): String = {
    // Load into a Jena model
    val model = ModelFactory.createDefaultModel()
    resource.read { inputStream =>
      RDFDataMgr.read(model, inputStream, RDFLanguages.filenameToLang(resource.name))
    }

    // Find find the ontology statement in the model
    val ontologyStatements = model.listStatements(null, model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), OWL2.Ontology)
    val vocabularyUris = ontologyStatements.asScala.map(_.getSubject.getURI).toSeq.distinct

    // Return the first (and only) vocabulary URI
    assert(vocabularyUris.size == 1, s"Expected exactly one vocabulary URI, but found ${vocabularyUris.size}.")
    vocabularyUris.head
  }

}
