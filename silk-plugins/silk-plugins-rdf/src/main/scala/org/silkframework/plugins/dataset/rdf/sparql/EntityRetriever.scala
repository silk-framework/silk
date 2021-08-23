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

import org.silkframework.dataset.rdf.{DataTypeLiteral, EntityRetrieverStrategy, LanguageLiteral, PlainLiteral, RdfNode, Resource, SparqlEndpoint}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.UUID

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
  def retrieve(entitySchema: EntitySchema, entities: Seq[Uri], limit: Option[Int])
              (implicit userContext: UserContext): Traversable[Entity]
}

/**
 * Factory for creating EntityRetriever entities.
 */
object EntityRetriever {

  /**
   * Creates a new EntityRetriever instance for specific strategy.
   */
  def apply(
   endpoint: SparqlEndpoint,
   strategy: EntityRetrieverStrategy = EntityRetrieverStrategy.parallel,
   pageSize: Int = 1000,
   graphUri: Option[String] = None,
   useOrderBy: Boolean = true
 ): EntityRetriever = {
    strategy match {
      case EntityRetrieverStrategy.simple =>
        new SimpleEntityRetriever(endpoint, pageSize, graphUri, useOrderBy, useSubSelect = false)
      case EntityRetrieverStrategy.subQuery =>
        new SimpleEntityRetriever(endpoint, pageSize, graphUri, useOrderBy, useSubSelect = true)
      case EntityRetrieverStrategy.parallel =>
        new ParallelEntityRetriever(endpoint, pageSize, graphUri, useOrderBy)
    }
  }

  private val charset: Charset = Charset.forName("UTF8")
  private def uniquePath(value: String): String = {
    if(value.size < 200) {
      URLEncoder.encode(value, "UTF-8")
    } else {
      UUID.nameUUIDFromBytes(value.getBytes(charset)).toString
    }
  }

  /** Extracts the subject URI from the SPARQL result. */
  def extractSubject(result: Map[String, RdfNode],
                     subjectVar: String): Option[String] = {
    result.get(subjectVar) match {
      case Some(Resource(value)) => Some(value)
      // Allow literals as subjects
      case Some(PlainLiteral(value)) => Some("urn:plainLiteral:" + uniquePath(value))
      case Some(LanguageLiteral(value, tag)) => Some(s"urn:languageLiteral:${uniquePath(tag)}_${uniquePath(value)}")
      case Some(DataTypeLiteral(value, dt)) => Some(s"urn:dataTypeLiteral:${uniquePath(dt)}_${uniquePath(value)}")
      case _ => None
    }
  }
}
