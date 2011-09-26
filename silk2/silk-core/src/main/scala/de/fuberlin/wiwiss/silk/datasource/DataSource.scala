package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.util.plugin.{PluginFactory, AnyPlugin}
import de.fuberlin.wiwiss.silk.instance.{SparqlRestriction, Path, InstanceSpecification, Instance}

/**
 * The base trait of a concrete source of instances.
 */
trait DataSource extends AnyPlugin {
  /**
   * Retrieves instances from this source which satisfy a specific instance specification.
   *
   * @param instanceSpec The instance specification
   * @param instances The URIs of the instances to be retrieved. If empty, all instances will be retrieved.
   *
   * @return A Traversable over the instances. The evaluation of the Traversable may be non-strict.
   */
  def retrieve(instanceSpec: InstanceSpecification, instances: Seq[String] = Seq.empty): Traversable[Instance]

  /**
   * Retrieves the most frequent paths in this source.
   * Implementations are only required to work on a best effort basis i.e. it does not necessarily return all paths in the source.
   * The default implementation returns an empty traversable.
   *
   * @param restrictions Only retrieve path on instances which satisfy the given restriction. If not given, all paths are retrieved.
   * @param depth Only retrieve paths up to a certain length. If not given, only paths of length 1 are returned.
   * @param limit Restricts the number of paths to be retrieved. If not given, all found paths are returned.
   *
   * @return A Traversable of the found paths and their frequency.
   */
  def retrievePaths(restrictions: SparqlRestriction = SparqlRestriction.empty, depth: Int = 1, limit: Option[Int] = None): Traversable[(Path, Double)] = {
    Traversable.empty
  }
}

object DataSource extends PluginFactory[DataSource]
