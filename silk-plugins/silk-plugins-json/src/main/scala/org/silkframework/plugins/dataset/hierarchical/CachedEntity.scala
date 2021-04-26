package org.silkframework.plugins.dataset.hierarchical


/**
  * A cached entity.
  *
  * @param uri The URI of the entity
  * @param values The values of the entity
  * @param tableIndex The index of the table this entity belongs to.
  */
private case class CachedEntity(uri: String, values: Seq[Seq[String]], tableIndex: Int)