package org.silkframework.plugins.dataset.rdf.vocab

import org.silkframework.dataset.rdf.{GraphStoreTrait, SparqlEndpoint}
import org.silkframework.rule.vocab.{Vocabulary, VocabularyManager}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.Identifier
import org.silkframework.workspace.WorkspaceFactory

import java.util.logging.Logger

@Plugin(
  id = "rdf",
  label = "RDF",
  description = "Loads vocabularies from the RDF workspace"
)
case class RdfVocabularyManager() extends VocabularyManager {

  private val log: Logger = Logger.getLogger(getClass.getName)

  override def get(uri: String, project: Option[Identifier])(implicit userContext: UserContext): Option[Vocabulary] = {
    workspaceSparqlEndpoint match {
      case Some(endpoint) =>
        new VocabularyLoader(endpoint).retrieveVocabulary(uri)
      case None =>
        // A missing SPARQL backend (e.g. the default file-based dev workspace) must degrade to
        // "vocabulary not available" instead of failing every vocabulary-dependent request
        // (alignment sync, transform saves, completions) with a runtime error.
        log.warning(s"Cannot load vocabulary '$uri': the workspace has no SPARQL enabled storage backend. " +
          "Configure a SPARQL-enabled workspace provider or a different 'vocabulary.manager.plugin'.")
        None
    }
  }

  private def workspaceSparqlEndpoint(implicit userContext: UserContext): Option[SparqlEndpoint with GraphStoreTrait] = {
    WorkspaceFactory().workspace.provider.sparqlEndpoint
  }

  override def retrieveGlobalVocabularies()(implicit userContext: UserContext): Option[Iterable[String]] = {
    // FIXME: No standard way of retrieving globally configured vocabularies.
    None
  }
}
