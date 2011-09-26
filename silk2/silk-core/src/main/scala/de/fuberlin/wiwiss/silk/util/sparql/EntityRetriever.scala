package de.fuberlin.wiwiss.silk.util.sparql

import de.fuberlin.wiwiss.silk.entity.{EntityDescription, Entity}

/**
 * Retrieves entities from a SPARQL endpoint.
 */
trait EntityRetriever {
  /**
   * Retrieves entities with a given entity description.
   *
   * @param entityDesc The entity description
   * @param entities The URIs of the entities to be retrieved. If empty, all entities will be retrieved.
   * @return The retrieved entities
   */
  def retrieve(entityDesc: EntityDescription, entities: Seq[String]): Traversable[Entity]
}

/**
 * Factory for creating EntityRetriever entities.
 */
object EntityRetriever {
  //Uses the parallel entity retriever by default as it is generally significantly faster.
  var useParallelRetriever = true

  /**
   * Creates a new EntityRetriever instance.
   */
  def apply(endpoint: SparqlEndpoint, pageSize: Int = 1000, graphUri: Option[String] = None): EntityRetriever = {
    if (useParallelRetriever)
      new ParallelEntityRetriever(endpoint, pageSize, graphUri)
    else
      new SimpleEntityRetriever(endpoint, pageSize, graphUri)
  }
}
