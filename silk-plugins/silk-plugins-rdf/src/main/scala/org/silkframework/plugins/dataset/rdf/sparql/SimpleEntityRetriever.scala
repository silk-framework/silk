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

import org.silkframework.dataset.rdf.{LanguageLiteral, RdfNode, SparqlEndpoint}
import org.silkframework.entity.rdf.{SparqlEntitySchema, SparqlPathBuilder}
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.{CloseableIterator, LegacyTraversable, Uri}

import java.util.logging.Logger

/**
 * EntityRetriever which executes a single SPARQL query to retrieve the entities.
 */
class SimpleEntityRetriever(endpoint: SparqlEndpoint,
                            pageSize: Int = SimpleEntityRetriever.DEFAULT_PAGE_SIZE,
                            graphUri: Option[String] = None,
                            useOrderBy: Boolean = true,
                            useSubSelect: Boolean = false,
                            useOptional: Boolean = true) extends EntityRetriever {
  private val log: Logger = Logger.getLogger(getClass.getName)
  private val varPrefix = "v"

  /**
   * Retrieves entities with a given entity description.
   *
   * @param entitySchema The entity description
   * @param entities The URIs of the entities to be retrieved. If empty, all entities will be retrieved.
   * @return The retrieved entities
   */
  override def retrieve(entitySchema: EntitySchema, entities: Seq[Uri], limit: Option[Int])
                       (implicit userContext: UserContext): CloseableIterator[Entity] = {
    retrieveAll(entitySchema, limit, entities)
  }

  /**
   * Retrieves all entities with a given entity description.
   *
   * @param entitySchema The entity schema
   * @return The retrieved entities
   */
  private def retrieveAll(entitySchema: EntitySchema, limit: Option[Int], entities: Seq[Uri])
                         (implicit userContext: UserContext): CloseableIterator[Entity] = {
    val sparqlEntitySchema = SparqlEntitySchema.fromSchema(entitySchema, entities)
    val sparqlQuery: String = buildSparqlQuery(sparqlEntitySchema, useDistinct = true)

    val sparqlResults = endpoint.select(sparqlQuery, limit.getOrElse(Int.MaxValue))

    new EntityTraversable(sparqlResults.bindings, entitySchema, limit, sparqlEntitySchema)
  }

  def buildSparqlQuery(sparqlEntitySchema: SparqlEntitySchema, useDistinct: Boolean): String = {
    //Select
    val sparql = new StringBuilder
    sparql append "SELECT "
    if(useDistinct) {
      sparql append "DISTINCT "
    }
    val selectVariables = genSelectVariables(sparqlEntitySchema)
    sparql append selectVariables
    sparql append "\n"

    addFrom(sparql)

    //Body
    sparql append "WHERE {\n"
    // GRAPH in subselect case
    for (graph <- graphUri if !graph.isEmpty && useSubSelect) sparql append s"GRAPH <$graph> {\n"

    addRestrictions(sparqlEntitySchema, sparql)

    sparql append SparqlPathBuilder(sparqlEntitySchema.paths, "?" + sparqlEntitySchema.variable, "?" + varPrefix, useOptional = useOptional)
    // End GRAPH in subselect case
    for (graph <- graphUri if !graph.isEmpty && useSubSelect) sparql append s"}"
    sparql append "}" // END WHERE
    if (useOrderBy) sparql append (" ORDER BY ?" + sparqlEntitySchema.variable)

    if (useSubSelect) {
      s"SELECT $selectVariables\nWHERE {\n${sparql.toString}\n}"
    } else {
      sparql.toString()
    }
  }

  private def addFrom(sparql: StringBuilder) = {
    // Graph. If the sub-select strategy should be used we have to use GRAPH instead of FROM
    for (graph <- graphUri if !graph.isEmpty && !useSubSelect) sparql append s"FROM <$graph>\n"
  }

  private def addRestrictions(sparqlEntitySchema: SparqlEntitySchema, sparql: StringBuilder) = {
    if (!sparqlEntitySchema.restrictions.toSparql.isEmpty) {
      sparql append (sparqlEntitySchema.restrictions.toSparql + "\n")
    } else {
      sparql append s"?${sparqlEntitySchema.variable} ?${varPrefix}_p ?${varPrefix}_o .\n"
    }
  }

  private def genSelectVariables(sparqlEntitySchema: SparqlEntitySchema) = {
    var temp = "?" + sparqlEntitySchema.variable + " "
    for (i <- sparqlEntitySchema.paths.indices) {
      temp += "?" + varPrefix + i + " "
    }
    temp
  }

  /**
   * Wraps a Traversable of SPARQL results and retrieves entities from them.
   */
  private class EntityTraversable(sparqlResults: CloseableIterator[Map[String, RdfNode]],
                                  entitySchema: EntitySchema,
                                  limit: Option[Int],
                                  sparqlEntitySchema: SparqlEntitySchema) extends LegacyTraversable[Entity] {
    // If path with a specific index is a language tag special path, also validates the special paths usage
    private val pathCharacteristicsMap: Map[Int, (Boolean, Boolean, Boolean)] = {
      entitySchema.typedPaths.zipWithIndex.map { case (typedPath, idx) =>
        val isLangSpecialPath = SparqlEntitySchema.specialPaths.isLangSpecialPath(typedPath)
        val isTextSpecialPath = SparqlEntitySchema.specialPaths.isTextSpecialPath(typedPath)
        val uriRequested = typedPath.valueType == ValueType.URI
        (idx, (isLangSpecialPath, isTextSpecialPath, uriRequested))
      }.toMap
    }

    private def isLangSpecialPath(pathIdx: Int): Boolean = {
      pathCharacteristicsMap.get(pathIdx).exists(_._1)
    }

    private def isTextSpecialPath(pathIdx: Int): Boolean = {
      pathCharacteristicsMap.get(pathIdx).exists(_._2)
    }

    private def uriRequestedForPath(pathIdx: Int): Boolean = {
      pathCharacteristicsMap.get(pathIdx).exists(_._3)
    }

    private val specialPathOnlyMap: Map[Int, Boolean] = {
      entitySchema.typedPaths.zipWithIndex.map { case (typedPath, idx) =>
        val isSpecialPathOnly = typedPath.size == 1 && (isTextSpecialPath(idx) || isLangSpecialPath(idx))
        (idx, isSpecialPathOnly)
      }.toMap
    }

    private def isSpecialPathOnly(pathIdx: Int): Boolean = {
      specialPathOnlyMap.getOrElse(pathIdx, false)
    }

    private def extractSpecialPathValue(pathIdx: Int, subject: RdfNode): String = {
      subject match {
        case LanguageLiteral(_, lang) if isLangSpecialPath(pathIdx) =>
          lang
        case rdfNode: RdfNode =>
          rdfNode.value
      }
    }

    override def foreach[U](f: Entity => U): Unit = {
      //Remember current subject
      var curSubject: Option[String] = None
      var curSubjectNode: Option[RdfNode] = None

      //Collect values of the current subject
      var values = Array.fill(entitySchema.typedPaths.size)(Seq[String]())

      // Count retrieved entities
      var counter = 0
      val startTime = System.currentTimeMillis()

      for (result <- sparqlResults) {
        //If the subject is unknown, find binding for subject variable
        //Check if we are still reading values for the current subject
        val resultSubject = EntityRetriever.extractSubject(result, sparqlEntitySchema.variable)

        if (resultSubject != curSubject) {
          for (curSubjectUri <- curSubject) {
            f(Entity(curSubjectUri, values.map(_.distinct).toIndexedSeq, entitySchema))
            counter += 1
            if(limit.exists(counter >= _)) {
              return
            }
          }
          curSubject = resultSubject
          curSubjectNode = result.get(sparqlEntitySchema.variable)
          values = Array.fill(entitySchema.typedPaths.size)(Seq[String]())
        }
        //Find results for values for the current subject
        if (curSubject.isDefined) {
          for(idx <- sparqlEntitySchema.paths.indices) {
            EntityRetriever.extractPathValue(
              curSubjectNode,
              result.get(varPrefix + idx),
              uriRequested = uriRequestedForPath(idx),
              isLangSpecialPath = isLangSpecialPath(idx),
              isSpecialPathOnly = isSpecialPathOnly(idx)
            ) foreach { value =>
              values(idx) = values(idx) :+ value
            }
          }
        }
      }

      for (curSubjectUri <- curSubject) {
        f(Entity(curSubjectUri, values.map(_.distinct).toIndexedSeq, entitySchema))
        counter += 1
      }

      log.info(s"Retrieved $counter entities of type '${entitySchema.typeUri}'" +
          s"${graphUri.map(g => s" from graph '$g'").getOrElse("")} in ${System.currentTimeMillis() - startTime}ms.")
    }
  }
}

object SimpleEntityRetriever {
  final val DEFAULT_PAGE_SIZE = 1000
}
