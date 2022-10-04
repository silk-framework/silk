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

import org.silkframework.dataset.rdf.{BlankNode, DataTypeLiteral, EntityRetrieverStrategy, LanguageLiteral, Literal, PlainLiteral, RdfNode, Resource, SparqlEndpoint}
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
    result.get(subjectVar).map(extractEntityUri)
  }

  /** Creates an entity URI based on the RDF node type. */
  def extractEntityUri(rdfNode: RdfNode): String = {
    rdfNode match {
      case Resource(value) => value
      // Allow literals as subjects
      case literal: Literal => literalToUri(literal)
      case blankNode: BlankNode => blankNodeToUri(blankNode)
    }
  }

  /** Convert literal to URI. */
  def literalToUri(literal: Literal): String = {
    literal match {
      case PlainLiteral(value) => "urn:plainLiteral:" + uniquePath(value)
      case LanguageLiteral(value, tag) => s"urn:languageLiteral:${uniquePath(tag)}_${uniquePath(value)}"
      case DataTypeLiteral(value, dt) => s"urn:dataTypeLiteral:${uniquePath(dt)}_${uniquePath(value)}"
    }
  }

  /** Convert a blank node to a URI. */
  def blankNodeToUri(blankNode: BlankNode): String = {
    "urn:blankNode:" + uniquePath(blankNode.value)
  }

  /**
    * Returns the requested value based on several path characteristics.
    *
    * @param subjectNode The entity that is retrieved.
    * @param objectNode The object RDF node of the requested path.
    * @param uriRequested If an URI is requested, i.e. a value type of URI.
    * @param isLangSpecialPath If this path requests a language tag.
    * @param isSpecialPathOnly If the path is a special path that is directly applied on the subject / entity.
    */
  def extractPathValue(subjectNode: Option[RdfNode],
                       objectNode: Option[RdfNode],
                       uriRequested: Boolean,
                       isLangSpecialPath: Boolean,
                       isSpecialPathOnly: Boolean): Option[String] = {
    if(isSpecialPathOnly) {
      extractValueFromSubject(subjectNode, isLangSpecialPath)
    } else {
      extractValueFromObject(objectNode, uriRequested, isLangSpecialPath)
    }
  }

  private def extractValueFromObject(objectNode: Option[RdfNode],
                                     uriRequested: Boolean,
                                     isLangSpecialPath: Boolean): Option[String] = {
    objectNode.map {
      case langLiteral: LanguageLiteral if isLangSpecialPath =>
        langLiteral.language
      case _: RdfNode if isLangSpecialPath =>
        return None
      case literal: Literal if uriRequested =>
        EntityRetriever.literalToUri(literal)
      case blankNode: BlankNode =>
        EntityRetriever.blankNodeToUri(blankNode)
      case rdfNode: RdfNode =>
        rdfNode.value
    }
  }

  private def extractValueFromSubject(subjectNode: Option[RdfNode],
                                      isLangSpecialPath: Boolean): Option[String] = {
    subjectNode flatMap {
      case LanguageLiteral(_, lang) if isLangSpecialPath =>
        Some(lang)
      case _: RdfNode if isLangSpecialPath =>
        None
      case rdfNode: RdfNode =>
        Some(rdfNode.value)
    }
  }
}
