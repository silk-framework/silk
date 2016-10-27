package org.silkframework.workspace.activity.transform

import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.vocab.{Vocabulary, VocabularyManager}
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.workspace.{ProjectTask, RdfWorkspaceProvider, User}
import org.silkframework.workspace.activity.PathsCacheTrait

/**
 * Holds the target vocabularies.
 */
class VocabularyCache(task: ProjectTask[TransformSpec]) extends Activity[Seq[Vocabulary]] with PathsCacheTrait {

  override def name: String = s"Vocabulary cache ${task.id}"

  override def initialValue: Option[Seq[Vocabulary]] = Some(Seq.empty)

  override def run(context: ActivityContext[Seq[Vocabulary]]): Unit = {
    val transform = task.data
    if(transform.targetVocabularies.nonEmpty) {
      val vocabManager = VocabularyManager(workspaceSparqlEndpoint())
      val vocabularies = for (vocab <- transform.targetVocabularies) yield vocabManager.get(vocab)
      context.value() = vocabularies
    }
  }

  private def workspaceSparqlEndpoint(): SparqlEndpoint = {
    User().workspace.provider match {
      case w: RdfWorkspaceProvider =>
        w.endpoint
      case _ =>
        throw new RuntimeException("Workspace has no SPARQL enabled storage backend.")
    }
  }

}
