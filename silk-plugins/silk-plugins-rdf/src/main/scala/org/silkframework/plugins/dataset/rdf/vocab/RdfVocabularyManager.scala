package org.silkframework.plugins.dataset.rdf.vocab

import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.rule.vocab.{Info, Vocabulary, VocabularyClass, VocabularyManager}
import org.silkframework.workspace.{RdfWorkspaceProvider, User}

case class RdfVocabularyManager() extends VocabularyManager {

  private lazy val loader = new VocabularyLoader(workspaceSparqlEndpoint)

  override def get(uri: String): Vocabulary = {
    Vocabulary(
      info = Info(uri, None, None),
      classes = loader.retrieveClasses(uri),
      properties = Traversable.empty
    )
  }

  private def workspaceSparqlEndpoint: SparqlEndpoint = {
    User().workspace.provider match {
      case w: RdfWorkspaceProvider =>
        w.endpoint
      case _ =>
        throw new RuntimeException("Workspace has no SPARQL enabled storage backend.")
    }
  }
}
