package org.silkframework.dataset

/**
 * An entity sink implements methods to write entities, e.g. the result of a transformation task.
 */
trait EntitySink extends DataSink {
  /**
   * Initializes this writer.
   *
   * @param properties The list of properties of the entities to be written.
   */
  def open(properties: Seq[String] = Seq.empty): Unit

  /**
   * Writes a new entity.
   *
   * @param subject The subject URI of the entity.
   * @param values The list of values of the entity. For each property that has been provided
   *               when opening this writer, it must contain a set of values.
   */
  def writeEntity(subject: String, values: Seq[Seq[String]]): Unit
}
