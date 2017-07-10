package org.silkframework.plugins.dataset.rdf.vocab

import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.rule.vocab.{GenericInfo$, Vocabulary, VocabularyManager}
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.Identifier
import org.silkframework.workspace.{RdfWorkspaceProvider, User}

@Plugin(
  id = "rdf",
  label = "RDF",
  description = "Loads vocabularies from the RDF workspace"
)
case class RdfVocabularyManager() extends VocabularyManager {

  private lazy val loader = new VocabularyLoader(workspaceSparqlEndpoint)

  override def get(uri: String, project: Identifier): Vocabulary = {
    loader.retrieveVocabulary(uri)
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
