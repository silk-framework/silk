package org.silkframework.plugins.dataset.rdf

import org.silkframework.plugins.dataset.rdf.datasets.{AlignmentDataset, InMemoryDataset, RdfFileDataset, SparqlDataset}
import org.silkframework.plugins.dataset.rdf.executors.{LocalSparqlCopyExecutor, LocalSparqlSelectExecutor, LocalSparqlUpdateExecutor}
import org.silkframework.plugins.dataset.rdf.tasks.{SparqlCopyCustomTask, SparqlSelectCustomTask, SparqlUpdateCustomTask}
import org.silkframework.plugins.dataset.rdf.vocab.{InMemoryVocabularyManager, RdfFilesVocabularyManager, RdfProjectFilesVocabularyManager, RdfVocabularyManager}
import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

class RdfPlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] =
    Seq(
      classOf[RdfFileDataset],
      classOf[SparqlDataset],
      classOf[AlignmentDataset],
      classOf[InMemoryDataset],
      classOf[RdfVocabularyManager],
      classOf[RdfFilesVocabularyManager],
      classOf[RdfProjectFilesVocabularyManager],
      classOf[InMemoryVocabularyManager],
      classOf[SparqlSelectCustomTask],
      classOf[SparqlCopyCustomTask],
      classOf[SparqlUpdateCustomTask]
    ) ++ executors

  val executors = Seq(
    classOf[LocalSparqlSelectExecutor],
    classOf[LocalSparqlUpdateExecutor],
    classOf[LocalSparqlCopyExecutor]
  )

}
