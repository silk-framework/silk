package org.silkframework.plugins.dataset.rdf

import org.silkframework.plugins.dataset.rdf.datasets.{AlignmentDataset, InMemoryDataset, RdfFileDataset, SparqlDataset}
import org.silkframework.plugins.dataset.rdf.executors.{LocalSparqlSelectExecutor, LocalSparqlUpdateExecutor}
import org.silkframework.plugins.dataset.rdf.tasks.{SparqlSelectCustomTask, SparqlUpdateCustomTask}
import org.silkframework.plugins.dataset.rdf.vocab.{RdfFilesVocabularyManager, RdfVocabularyManager}
import org.silkframework.runtime.plugin.PluginModule

class RdfPlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_]] =
    Seq(
      classOf[RdfFileDataset],
      classOf[SparqlDataset],
      classOf[AlignmentDataset],
      classOf[InMemoryDataset],
      classOf[RdfVocabularyManager],
      classOf[RdfFilesVocabularyManager],
      classOf[SparqlSelectCustomTask],
      classOf[SparqlUpdateCustomTask]
    ) ++ executors

  val executors = Seq(
    classOf[LocalSparqlSelectExecutor],
    classOf[LocalSparqlUpdateExecutor]
  )

}
