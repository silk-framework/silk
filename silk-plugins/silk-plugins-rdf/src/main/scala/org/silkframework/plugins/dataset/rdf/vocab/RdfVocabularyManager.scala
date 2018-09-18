package org.silkframework.plugins.dataset.rdf.vocab

import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.rule.vocab.{Vocabulary, VocabularyManager}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.Identifier
import org.silkframework.workspace.{RdfWorkspaceProvider, WorkspaceFactory}

@Plugin(
  id = "rdf",
  label = "RDF",
  description = "Loads vocabularies from the RDF workspace"
)
case class RdfVocabularyManager() extends VocabularyManager {

  private def loader(implicit userContext: UserContext) = new VocabularyLoader(workspaceSparqlEndpoint)

  override def get(uri: String, project: Identifier)(implicit userContext: UserContext): Vocabulary = {
    loader.retrieveVocabulary(uri)
  }

  private def workspaceSparqlEndpoint(implicit userContext: UserContext): SparqlEndpoint = {
    WorkspaceFactory().workspace.provider match {
      case w: RdfWorkspaceProvider =>
        w.endpoint
      case _ =>
        throw new RuntimeException("Workspace has no SPARQL enabled storage backend.")
    }
  }
}
