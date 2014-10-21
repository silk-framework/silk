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

package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.sparql

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.dataset.rdf.{RdfNode, Resource, SparqlEndpoint}
import de.fuberlin.wiwiss.silk.entity.{Entity, EntityDescription, Path}

/**
 * EntityRetriever which executes multiple SPARQL queries (one for each property path) in parallel and merges the results into single entities.
 */
class ParallelEntityRetriever(endpoint: SparqlEndpoint, pageSize: Int = 1000, graphUri: Option[String] = None, useOrderBy: Boolean = false) extends EntityRetriever {
  private val varPrefix = "v"

  private val maxQueueSize = 1000

  private val logger = Logger.getLogger(classOf[ParallelEntityRetriever].getName)

  @volatile private var canceled = false

  /**
   * Retrieves entities with a given entity description.
   *
   * @param entityDesc The entity description
   * @param entities The URIs of the entities to be retrieved. If empty, all entities will be retrieved.
   * @return The retrieved entities
   */
  override def retrieve(entityDesc: EntityDescription, entities: Seq[String]): Traversable[Entity] = {
    canceled = false
    if(entityDesc.paths.size <= 1)
      new SimpleEntityRetriever(endpoint, pageSize, graphUri).retrieve(entityDesc, entities)
    else
      new EntityTraversable(entityDesc, entities)
  }

  /**
   * Wraps a Traversable of SPARQL results and retrieves entities from them.
   */
  private class EntityTraversable(entityDesc: EntityDescription, entityUris: Seq[String]) extends Traversable[Entity] {
    override def foreach[U](f: Entity => U) {
      var inconsistentOrder = false
      var counter = 0

      val pathRetrievers = for (path <- entityDesc.paths) yield new PathRetriever(entityUris, entityDesc, path)

      pathRetrievers.foreach(_.start())

      try {
        while (pathRetrievers.forall(_.hasNext) && !inconsistentOrder) {
          val pathValues = for (pathRetriever <- pathRetrievers) yield pathRetriever.next()

          val uri = pathValues.head.uri
          if (pathValues.tail.forall(_.uri == uri)) {
            f(new Entity(uri, pathValues.map(_.values).toIndexedSeq, entityDesc))
            counter += 1
          }
          else {
            inconsistentOrder = true
            canceled = true
          }
        }
      }
      catch {
        case ex: InterruptedException => {
          logger.log(Level.INFO, "Canceled retrieving entities for '" + entityDesc.restrictions + "'")
          canceled = true
        }
        case ex: Exception => {
          logger.log(Level.WARNING, "Failed to execute query for '" + entityDesc.restrictions + "'", ex)
          canceled = true
        }
      }

      if (inconsistentOrder) {
        if (!useOrderBy) {
          logger.info("Querying endpoint '" + endpoint + "' without order-by failed. Using order-by.")
          val entityRetriever = new ParallelEntityRetriever(endpoint, pageSize, graphUri, true)
          val entities = entityRetriever.retrieve(entityDesc, entityUris)
          entities.drop(counter).foreach(f)
        }
        else {
          logger.warning("Cannot execute queries in parallel on '" + endpoint + "' because the endpoint returned the results in different orders even when using order-by. Falling back to serial querying.")
          val simpleEntityRetriever = new SimpleEntityRetriever(endpoint, pageSize, graphUri)
          val entities = simpleEntityRetriever.retrieve(entityDesc, entityUris)
          entities.drop(counter).foreach(f)
        }
      }
    }
  }

  private class PathRetriever(entityUris: Seq[String], entityDesc: EntityDescription, path: Path) extends Thread {
    private val queue = new ConcurrentLinkedQueue[PathValues]()

    @volatile private var exception: Throwable = null

    def hasNext: Boolean = {
      //If the queue is empty, wait until an element has been read
      while (queue.isEmpty && isAlive) {
        Thread.sleep(100)
      }

      //Throw exceptions which occurred during querying
      if (exception != null) throw exception

      !queue.isEmpty
    }

    def next(): PathValues = {
      //Throw exceptions which occurred during querying
      if (exception != null) throw exception

      queue.remove()
    }

    override def run() {
      try {
        if (entityUris.isEmpty) {
          //Query for all entities
          val sparqlResults = queryPath()
          parseResults(sparqlResults.bindings)
        }
        else {
          //Query for a list of entities
          for (entityUri <- entityUris) {
            val sparqlResults = queryPath(Some(entityUri))
            parseResults(sparqlResults.bindings, Some(entityUri))
          }
        }
      }
      catch {
        case ex: Throwable => exception = ex
      }
    }

    private def queryPath(fixedSubject: Option[String] = None) = {
      //Select
      var sparql = "SELECT "
      if (fixedSubject.isEmpty) {
        sparql += "?" + entityDesc.variable + " "
      }
      sparql += "?" + varPrefix + "0\n"

      //Graph
      for (graph <- graphUri if !graph.isEmpty) sparql += "FROM <" + graph + ">\n"

      //Body
      sparql += "WHERE {\n"
      fixedSubject match {
        case Some(subjectUri) => {
          sparql += SparqlPathBuilder(path :: Nil, "<" + subjectUri + ">", "?" + varPrefix)
        }
        case None => {
          if (entityDesc.restrictions.toSparql.isEmpty)
            sparql += "?" + entityDesc.variable + " ?" + varPrefix + "_p ?" + varPrefix + "_o .\n"
          else
            sparql += entityDesc.restrictions.toSparql + "\n"
          sparql += SparqlPathBuilder(path :: Nil, "?" + entityDesc.variable, "?" + varPrefix)
        }
      }
      sparql += "}"

      if (useOrderBy && fixedSubject.isEmpty) {
        sparql += " ORDER BY " + "?" + entityDesc.variable
      }

      endpoint.query(sparql)
    }

    private def parseResults(sparqlResults: Traversable[Map[String, RdfNode]], fixedSubject: Option[String] = None) {
      var currentSubject: Option[String] = fixedSubject
      var currentValues: Set[String] = Set.empty

      for (result <- sparqlResults) {
        if (canceled) {
          return
        }

        if (!fixedSubject.isDefined) {
          //Check if we are still reading values for the current subject
          val subject = result.get(entityDesc.variable) match {
            case Some(Resource(value)) => Some(value)
            case _ => None
          }

          if (currentSubject.isEmpty) {
            currentSubject = subject
          } else if (subject.isDefined && subject != currentSubject) {
            while (queue.size > maxQueueSize && !canceled) {
              Thread.sleep(100)
            }

            queue.add(PathValues(currentSubject.get, currentValues))

            currentSubject = subject
            currentValues = Set.empty
          }
        }

        if (currentSubject.isDefined) {
          for (node <- result.get(varPrefix + "0")) {
            currentValues += node.value
          }
        }
      }

      for (s <- currentSubject if !sparqlResults.isEmpty) {
        queue.add(PathValues(s, currentValues))
      }
    }
  }

  private case class PathValues(uri: String, values: Set[String])

}
