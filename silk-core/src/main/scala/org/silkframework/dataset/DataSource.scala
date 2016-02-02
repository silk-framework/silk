/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.dataset

import java.net.URI

import org.silkframework.entity._
import org.silkframework.entity.rdf.{SparqlRestriction, SparqlEntitySchema}
import org.silkframework.util.{SampleUtil, Uri}
import scala.reflect.ClassTag
import scala.util.Random

/**
 * The base trait of a concrete source of entities.
 */
trait DataSource {
  /**
   * Retrieves known types in this source.
   * Implementations are only required to work on a best effort basis i.e. it does not necessarily return any or all types.
   * The default implementation returns an empty traversable.
   *
   * @param limit Restricts the number of types to be retrieved. If not given, all found types are returned.
   *
   */
  def retrieveTypes(limit: Option[Int] = None): Traversable[(String, Double)] = {
    Traversable.empty
  }

  /**
   * Retrieves the most frequent paths in this source.
   * Implementations are only required to work on a best effort basis i.e. it does not necessarily return all paths in the source.
   * The default implementation returns an empty traversable.
   *
   * @param t The entity type for which paths shall be retrieved
   * @param depth Only retrieve paths up to a certain length. If not given, only paths of length 1 are returned.
   * @param limit Restricts the number of paths to be retrieved. If not given, all found paths are returned.
   *
   * @return A Sequence of the found paths sorted by their frequency (most frequent first).
   */
  def retrievePaths(t: Uri, depth: Int = 1, limit: Option[Int] = None): IndexedSeq[Path] = {
    IndexedSeq.empty
  }

  /**
   * Retrieves entities from this source which satisfy a specific entity schema.
   *
   * @param entitySchema The entity schema
   * @param limit Limits the maximum number of retrieved entities
   *
   * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
   */
  def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None): Traversable[Entity] = ???

  /**
   * Retrieves a list of entities from this source.
   *
   * @param entitySchema The entity schema
   * @param entities The URIs of the entities to be retrieved.
   *
   * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
   */
  def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Option[Entity]] = ???

  /**
   * Retrieves entities from this source which satisfy a specific entity description.
   *
   * @param entityDesc The entity description
   * @param entities The URIs of the entities to be retrieved. If empty, all entities will be retrieved.
   *
   * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
   */
  def retrieveSparqlEntities(entityDesc: SparqlEntitySchema, entities: Seq[String] = Seq.empty): Traversable[Entity]

  /**
   * Retrieves the most frequent paths in this source.
   * Implementations are only required to work on a best effort basis i.e. it does not necessarily return all paths in the source.
   * The default implementation returns an empty traversable.
   *
   * @param restriction Only retrieve path on entities which satisfy the given restriction. If not given, all paths are retrieved.
   * @param depth Only retrieve paths up to a certain length. If not given, only paths of length 1 are returned.
   * @param limit Restricts the number of paths to be retrieved. If not given, all found paths are returned.
   *
   * @return A Traversable of the found paths and their frequency.
   */
  def retrieveSparqlPaths(restriction: SparqlRestriction = SparqlRestriction.empty, depth: Int = 1, limit: Option[Int] = None): Traversable[(Path, Double)] = {
    Traversable.empty
  }

  /**
   * Samples a fixed size set of entities from the whole dataset.
   * The default implementation iterates once over all entities.
   * @param entityDesc
   * @param size
   * @return
   */
  def sampleEntities(entityDesc: EntitySchema,
                     size: Int,
                     filterOpt: Option[Entity => Boolean]): Seq[Entity] = {
    val entities = retrieve(entityDesc)
    SampleUtil.sample(entities, size, filterOpt)
  }

  def sampleEntities(entityDesc: SparqlEntitySchema,
                     size: Int,
                     filterOpt: Option[Entity => Boolean]): Seq[Entity] = {
    val entities = retrieveSparqlEntities(entityDesc)
    SampleUtil.sample(entities, size, filterOpt)
  }
}