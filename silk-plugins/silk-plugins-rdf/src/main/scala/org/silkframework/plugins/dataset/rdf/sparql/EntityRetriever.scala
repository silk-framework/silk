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

package org.silkframework.plugins.dataset.rdf.sparql

import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.util.Uri

/**
 * Retrieves entities from a SPARQL endpoint.
 */
trait EntityRetriever {
  /**
   * Retrieves entities with a given entity description.
   *
   * @param entitySchema The entity schema
   * @param entities The URIs of the entities to be retrieved. If empty, all entities will be retrieved.
   * @return The retrieved entities
   */
  def retrieve(entitySchema: EntitySchema, entities: Seq[Uri], limit: Option[Int]): Traversable[Entity]
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
