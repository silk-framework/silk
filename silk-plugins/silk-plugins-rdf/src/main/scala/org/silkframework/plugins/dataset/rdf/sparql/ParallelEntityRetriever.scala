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

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.{Level, Logger}

import org.silkframework.dataset.rdf.{RdfNode, Resource, SparqlEndpoint}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.rdf.{SparqlEntitySchema, SparqlPathBuilder, SparqlRestriction}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
 * EntityRetriever which executes multiple SPARQL queries (one for each property path) in parallel and merges the results into single entities.
 */
class ParallelEntityRetriever(endpoint: SparqlEndpoint,
                              pageSize: Int = SimpleEntityRetriever.DEFAULT_PAGE_SIZE,
                              graphUri: Option[String] = None,
                              useOrderBy: Boolean = false) extends EntityRetriever {
  private val varPrefix = "v"

  private val maxQueueSize = 1000

  private val logger = Logger.getLogger(classOf[ParallelEntityRetriever].getName)

  @volatile private var canceled = false

  /**
   * Retrieves entities with a given entity description.
   *
   * @param entitySchema The entity description
   * @param entities The URIs of the entities to be retrieved. If empty, all entities will be retrieved.
   * @return The retrieved entities
   */
  override def retrieve(entitySchema: EntitySchema, entities: Seq[Uri], limit: Option[Int])
                       (implicit userContext: UserContext): Traversable[Entity] = {
    canceled = false
    if(entitySchema.typedPaths.size <= 1) {
      new SimpleEntityRetriever(endpoint, pageSize, graphUri, useOrderBy).retrieve(entitySchema, entities, limit)
    } else {
      new EntityTraversable(entitySchema, entities, limit)
    }
  }

  /**
   * Wraps a Traversable of SPARQL results and retrieves entities from them.
   */
  private class EntityTraversable(entitySchema: EntitySchema, entityUris: Seq[Uri], limit: Option[Int])
                                 (implicit userContext: UserContext)extends Traversable[Entity] {
    override def foreach[U](f: Entity => U): Unit = {
      var inconsistentOrder = false
      var counter = 0
      val startTime = System.currentTimeMillis()

      val pathRetrievers = for (typedPath <- entitySchema.typedPaths) yield {
        new PathRetriever(entityUris, SparqlEntitySchema.fromSchema(entitySchema, entityUris), typedPath.toSimplePath, limit)
      }

      pathRetrievers.foreach(_.start())

      try {
        while (pathRetrievers.forall(_.hasNext) && !inconsistentOrder && limit.forall(counter <= _)) {
          val pathValues = for (pathRetriever <- pathRetrievers) yield pathRetriever.next()

          val uri = pathValues.head.uri
          if (pathValues.tail.forall(_.uri == uri)) {
            f(Entity(uri, pathValues.map(_.values).toIndexedSeq, entitySchema))
            counter += 1
          }
          else {
            inconsistentOrder = true
            canceled = true
          }
        }
        logger.info(s"Retrieved $counter entities of type '${entitySchema.typeUri}'" +
            s"${graphUri.map(g => s" from graph '$g'").getOrElse("")} in ${System.currentTimeMillis() - startTime}ms.")
      }
      catch {
        case ex: InterruptedException =>
          logger.log(Level.INFO, "Canceled retrieving entities for '" + entitySchema.typeUri + "'")
          canceled = true
        case ex: Exception =>
          logger.log(Level.WARNING, "Failed to execute query for '" + entitySchema.typeUri + "'", ex)
          canceled = true
      }

      if (inconsistentOrder) {
        handleInconsistentOrder(f, counter)
      }
    }

    private def handleInconsistentOrder[U](f: Entity => U, counter: Int): Unit = {
      if (!useOrderBy) {
        logger.info("Querying endpoint '" + endpoint + "' without order-by failed. Using order-by.")
        val entityRetriever = new ParallelEntityRetriever(endpoint, pageSize, graphUri, true)
        val entities = entityRetriever.retrieve(entitySchema, entityUris, limit)
        entities.drop(counter).foreach(f)
      }
      else {
        logger.warning("Cannot execute queries in parallel on '" + endpoint + "' because the endpoint returned the " +
            "results in different orders even when using order-by. Falling back to serial querying.")
        val simpleEntityRetriever = new SimpleEntityRetriever(endpoint, pageSize, graphUri)
        val entities = simpleEntityRetriever.retrieve(entitySchema, entityUris, limit)
        entities.drop(counter).foreach(f)
      }
    }
  }

  private class PathRetriever(entityUris: Seq[Uri], entityDesc: SparqlEntitySchema, path: UntypedPath, limit: Option[Int])
                             (implicit userContext: UserContext)extends Thread {
    private val queue = new ConcurrentLinkedQueue[PathValues]()

    @volatile private var exception: Throwable = _

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
        //Query for all entities
        val sparqlResults = queryPath()
        parseResults(sparqlResults.bindings)
      }
      catch {
        case ex: Throwable => exception = ex
      }
    }

    private def queryPath()(implicit userContext: UserContext) = {
      val sparqlQuery = ParallelEntityRetriever.pathQuery(entityDesc.variable, entityDesc.restrictions, path,
        useDistinct = true, graphUri = graphUri, useOrderBy = useOrderBy, varPrefix = varPrefix)

      endpoint.select(sparqlQuery, limit.getOrElse(Int.MaxValue))
    }

    private def parseResults(sparqlResults: Traversable[Map[String, RdfNode]], fixedSubject: Option[Uri] = None): Unit = {
      var currentSubject: Option[String] = fixedSubject.map(_.uri)
      var currentValues: Seq[String] = Seq.empty

      for (result <- sparqlResults) {
        if (canceled) {
          return
        }

        if (fixedSubject.isEmpty) {
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
            currentValues = Seq.empty
          }
        }

        if (currentSubject.isDefined) {
          for (node <- result.get(varPrefix + "0")) {
            currentValues = currentValues :+ node.value
          }
        }
      }

      for (s <- currentSubject if sparqlResults.nonEmpty) {
        queue.add(PathValues(s, currentValues))
      }
    }
  }

  private case class PathValues(uri: String, values: Seq[String])

}

object ParallelEntityRetriever {
  /** Returns a query to access values of a single path of a resource.
    *
    * @param subjectVar  The variable name of the subject
    * @param restriction A SPARQL restriction defined on the subject. Must have the same variable name as the subjectVar
    * @param path        The path that should be retrieved
    * @param useDistinct Get distinct values. This can only happen with multi-hop paths.
    * @param graphUri    An optional graph URI
    * @param useOrderBy  Should the results be ordered by the subjectVar
    * @param varPrefix   The variable prefix. The result value can be accessed via varPrefix + "0"
    */
  def pathQuery(subjectVar: String,
                restriction: SparqlRestriction,
                path: UntypedPath,
                useDistinct: Boolean,
                graphUri: Option[String],
                useOrderBy: Boolean,
                varPrefix: String): String = {
    //Select
    val sparql = new StringBuilder
    sparql append "SELECT "
    if(useDistinct) {
      sparql append "DISTINCT "
    }

    sparql append "?" + subjectVar + " "
    sparql append "?" + varPrefix + "0\n"

    //Graph
    for (graph <- graphUri if !graph.isEmpty) sparql append "FROM <" + graph + ">\n"

    //Body
    sparql append "WHERE {\n"


    if (restriction.toSparql.isEmpty) {
      sparql append "?" + subjectVar + " ?" + varPrefix + "_p ?" + varPrefix + "_o .\n"
    } else {
      sparql append restriction.toSparql + "\n"
    }
    sparql append SparqlPathBuilder(path :: Nil, "?" + subjectVar, "?" + varPrefix)

    sparql append "}" // END WHERE

    if (useOrderBy) {
      sparql append " ORDER BY " + "?" + subjectVar
    }
    sparql.toString
  }
}