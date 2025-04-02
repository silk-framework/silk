package org.silkframework.plugins.dataset.rdf.vocab

import org.silkframework.dataset.rdf.{GraphStoreTrait, SparqlEndpoint}
import org.silkframework.rule.vocab.{Vocabulary, VocabularyManager}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.Identifier
import org.silkframework.workspace.WorkspaceFactory

@Plugin(
  id = "rdf",
  label = "RDF",
  description = "Loads vocabularies from the RDF workspace"
)
case class RdfVocabularyManager() extends VocabularyManager {

  private def loader(implicit userContext: UserContext) = new VocabularyLoader(workspaceSparqlEndpoint)

  override def get(uri: String, project: Option[Identifier])(implicit userContext: UserContext): Option[Vocabulary] = {
    loader.retrieveVocabulary(uri)
  }

  private def workspaceSparqlEndpoint(implicit userContext: UserContext): SparqlEndpoint with GraphStoreTrait = {
    WorkspaceFactory().workspace.provider.sparqlEndpoint match {
      case Some(endpoint) =>
        endpoint
      case _ =>
        throw new RuntimeException("Workspace has no SPARQL enabled storage backend.")
    }
  }

  override def retrieveGlobalVocabularies()(implicit userContext: UserContext): Option[Iterable[String]] = {
    // FIXME: No standard way of retrieving globally configured vocabularies.
    None
  }
}
