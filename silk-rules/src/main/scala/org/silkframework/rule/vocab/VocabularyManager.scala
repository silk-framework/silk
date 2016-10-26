package org.silkframework.rule.vocab

import org.silkframework.dataset.rdf.SparqlEndpoint

trait VocabularyManager {

  def get(uri: String): Vocabulary

}

class RdfVocabularyManager(endpoint: SparqlEndpoint) extends VocabularyManager {

  override def get(uri: String): Vocabulary = ???

}

object VocabularyManager {

  def apply(sparqlEndpoint: SparqlEndpoint): VocabularyManager = {
    new RdfVocabularyManager(sparqlEndpoint)
  }

}
