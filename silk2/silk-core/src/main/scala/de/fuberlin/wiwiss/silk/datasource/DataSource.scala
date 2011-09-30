package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.util.plugin.{PluginFactory, AnyPlugin}
import de.fuberlin.wiwiss.silk.entity.{Path, SparqlRestriction, Entity, EntityDescription}

/**
 * The base trait of a concrete source of entities.
 */
trait DataSource extends AnyPlugin {
  /**
   * Retrieves entities from this source which satisfy a specific entity description.
   *
   * @param entityDesc The entity description
   * @param entities The URIs of the entities to be retrieved. If empty, all entities will be retrieved.
   *
   * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
   */
  def retrieve(entityDesc: EntityDescription, entities: Seq[String] = Seq.empty): Traversable[Entity]

  /**
   * Retrieves the most frequent paths in this source.
   * Implementations are only required to work on a best effort basis i.e. it does not necessarily return all paths in the source.
   * The default implementation returns an empty traversable.
   *
   * @param restrictions Only retrieve path on entities which satisfy the given restriction. If not given, all paths are retrieved.
   * @param depth Only retrieve paths up to a certain length. If not given, only paths of length 1 are returned.
   * @param limit Restricts the number of paths to be retrieved. If not given, all found paths are returned.
   *
   * @return A Traversable of the found paths and their frequency.
   */
  def retrievePaths(restrictions: SparqlRestriction = SparqlRestriction.empty, depth: Int = 1, limit: Option[Int] = None): Traversable[(Path, Double)] = {
    Traversable.empty
  }
}

/**
 * Creates new data source instances.
 */
object DataSource extends PluginFactory[DataSource]
