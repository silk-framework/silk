package org.silkframework.plugins.dataset.rdf

import org.silkframework.plugins.dataset.rdf.datasets.{AlignmentDataset, AlignmentDatasetExecutor, InMemoryDataset, InMemoryDatasetExecutor, JenaModelDatasetExecutor, RdfFileDataset, RdfFileDatasetExecutor, RdfInMemoryDatasetExecutor, SparqlDataset, SparqlDatasetExecutor}
import org.silkframework.plugins.dataset.rdf.executors.{LocalSparqlCopyExecutor, LocalSparqlSelectExecutor, LocalSparqlUpdateExecutor}
import org.silkframework.plugins.dataset.rdf.tasks.{SparqlCopyCustomTask, SparqlSelectCustomTask, SparqlUpdateCustomTask}
import org.silkframework.plugins.dataset.rdf.tasks.templating.SparqlSimpleTemplateEngine
import org.silkframework.plugins.dataset.rdf.vocab.{InMemoryVocabularyManager, RdfFilesVocabularyManager, RdfProjectFilesVocabularyManager, RdfVocabularyManager}
import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

class RdfPlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] =
    Seq(
      // Datasets with their executors
      classOf[RdfFileDataset],
      classOf[RdfFileDatasetExecutor],
      classOf[SparqlDataset],
      classOf[SparqlDatasetExecutor],
      classOf[AlignmentDataset],
      classOf[AlignmentDatasetExecutor],
      classOf[InMemoryDataset],
      classOf[InMemoryDatasetExecutor],
      // Executors whose dataset plugin is not registered (programmatic / serialization-only)
      classOf[RdfInMemoryDatasetExecutor],
      classOf[JenaModelDatasetExecutor],
      // SPARQL custom tasks with their executors
      classOf[SparqlSelectCustomTask],
      classOf[LocalSparqlSelectExecutor],
      classOf[SparqlCopyCustomTask],
      classOf[LocalSparqlCopyExecutor],
      classOf[SparqlUpdateCustomTask],
      classOf[LocalSparqlUpdateExecutor],
      // Vocabulary managers
      classOf[RdfVocabularyManager],
      classOf[RdfFilesVocabularyManager],
      classOf[RdfProjectFilesVocabularyManager],
      classOf[InMemoryVocabularyManager],
      // Templating
      classOf[SparqlSimpleTemplateEngine]
    )

}
