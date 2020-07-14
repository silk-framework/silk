package org.silkframework.runtime.plugin

import java.nio.charset.Charset

import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.WorkspaceReadTrait
import scala.collection.JavaConverters._

/**
  * Autocompletion provider that completes available charsets.
  * Only suggest primary names and ignores aliases (including aliases currently spams the UI with too many similar names).
  */
case class CharsetAutocompletionProvider() extends PluginParameterAutoCompletionProvider {

  override def autoComplete(searchQuery: String, projectId: String, dependOnParameterValues: Seq[String],
                            workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    val multiSearchWords = extractSearchTerms(searchQuery)
    CharsetAutocompletionProvider.charsets
      .filter(r => matchesSearchTerm(multiSearchWords, r.toLowerCase))
      .map(r => AutoCompletionResult(r, None))
  }

  override def valueToLabel(projectId: String, value: String, dependOnParameterValues: Seq[String],
                            workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Option[String] = {
    None
  }
}

object CharsetAutocompletionProvider {

  lazy val charsets: Seq[String] = {
    Charset.availableCharsets().keySet.asScala.toSeq.sorted
  }

}
