package org.silkframework.dataset

import org.silkframework.config.Prefixes
import org.silkframework.entity.Link
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
 * An empty data set.
 */
object EmptyDataset extends Dataset with Serializable {

  /**
    * Clears the contents of this dataset.
    */
  def clear(): Unit = { }

  /**
   * Returns an empty data source.
   */
  override def source(implicit userContext: UserContext): DataSource = EmptySource

  /**
   * Returns a dummy entity sink.
   */
  override def entitySink(implicit userContext: UserContext): EntitySink = new EntitySink {
    override def writeEntity(subject: String, values: IndexedSeq[Seq[String]])
                            (implicit userContext: UserContext): Unit = {}

    /**
     * Initializes this writer.
     *
     * @param properties The list of properties of the entities to be written.
     */
    override def openTable(typeUri: Uri, properties: Seq[TypedProperty], singleEntity: Boolean = false)
                          (implicit userContext: UserContext, prefixes: Prefixes): Unit = {}

    override def closeTable()(implicit userContext: UserContext): Unit = {}

    override def close()(implicit userContext: UserContext): Unit = {}

    /**
      * Makes sure that the next write will start from an empty dataset.
      * Does nothing as this dataset is always empty
      */
    override def clear(force: Boolean = false)(implicit userContext: UserContext): Unit = {}
  }

  /**
   * Returns a dummy link sink
   */
  override def linkSink(implicit userContext: UserContext): LinkSink = new LinkSink {
    /**
     * Initialize the link sink
     */
    override def init()(implicit userContext: UserContext, prefixes: Prefixes): Unit = {}

    /**
     * Writes a new link to this writer.
     */
    override def writeLink(link: Link, predicateUri: String, inversePredicateUri: Option[String])
                          (implicit userContext: UserContext, prefixes: Prefixes): Unit = {}

    override def close()(implicit userContext: UserContext): Unit = {}

    /**
      * Makes sure that the next write will start from an empty dataset.
      * Does nothing as this dataset is always empty
      */
    override def clear(force: Boolean = false)(implicit userContext: UserContext): Unit = {}
  }

  override def characteristics: DatasetCharacteristics = DatasetCharacteristics.attributesOnly()
}
