package org.silkframework.dataset

import org.silkframework.entity.Link
import org.silkframework.entity.rdf.SparqlEntitySchema

/**
 * An empty data set.
 */
private object EmptyDataset extends DatasetPlugin {

  /**
   * Returns an empty data source.
   */
  override def source: DataSource = new DataSource {
    override def retrieveSparqlEntities(entityDesc: SparqlEntitySchema, entities: Seq[String]) = Traversable.empty
  }

  /**
   * Returns a dummy data sink.
   */
  override def sink: DataSink = new DataSink {
    override def writeLink(link: Link, predicateUri: String): Unit = {}
    override def writeEntity(subject: String, values: Seq[Seq[String]]): Unit = {}
  }
}
