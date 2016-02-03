package org.silkframework.dataset

import org.silkframework.entity.{Entity, EntitySchema, Link}
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.util.Uri

/**
 * An empty data set.
 */
private object EmptyDataset extends DatasetPlugin {

  /**
    * Clears the contents of this dataset.
    */
  def clear(): Unit = { }

  /**
   * Returns an empty data source.
   */
  override def source: DataSource = new DataSource {
    override def retrieve(entitySchema: EntitySchema, limit: Option[Int]) = Traversable[Entity]()
    override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = Seq.empty
  }

  /**
   * Returns a dummy entity sink.
   */
  override def entitySink: EntitySink = new EntitySink {
    override def writeEntity(subject: String, values: Seq[Seq[String]]): Unit = {}

    /**
     * Initializes this writer.
     *
     * @param properties The list of properties of the entities to be written.
     */
    override def open(properties: Seq[String]): Unit = {}

    override def close(): Unit = {}
  }

  /**
   * Returns a dummy link sink
   */
  override def linkSink: LinkSink = new LinkSink {
    /**
     * Initialize the link sink
     */
    override def init(): Unit = {}

    /**
     * Writes a new link to this writer.
     */
    override def writeLink(link: Link, predicateUri: String): Unit = {}

    override def close(): Unit = {}
  }
}
