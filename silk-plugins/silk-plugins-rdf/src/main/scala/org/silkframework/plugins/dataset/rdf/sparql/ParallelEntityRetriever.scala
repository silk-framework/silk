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

import org.silkframework.dataset.rdf._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.rdf.SparqlEntitySchema.specialPaths
import org.silkframework.entity.rdf.{SparqlEntitySchema, SparqlPathBuilder, SparqlRestriction}
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.plugins.dataset.rdf.sparql.ParallelEntityRetriever.{ExceptionPathValues, ExistingPathValues, PathValues, QueueEndMarker}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.iterator.{CloseableIterator, TraversableIterator}
import org.silkframework.util.Uri

import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import java.util.logging.{Level, Logger}

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
                       (implicit userContext: UserContext): CloseableIterator[Entity] = {
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
                                 (implicit userContext: UserContext) extends TraversableIterator[Entity] {
    override def foreach[U](f: Entity => U): Unit = {
      var inconsistentOrder = false
      var counter = 0
      val startTime = System.currentTimeMillis()

      val pathRetrievers = for (typedPath <- entitySchema.typedPaths) yield {
        new PathRetriever(SparqlEntitySchema.fromSchema(entitySchema, entityUris), typedPath, limit)
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
        case _: InterruptedException =>
          logger.log(Level.INFO, "Canceled retrieving entities for '" + entitySchema.typeUri + "'")
          canceled = true
        case ex: Exception =>
          logger.log(Level.WARNING, "Failed to execute query for '" + entitySchema.typeUri + "'", ex)
          throw ex
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

  private class PathRetriever(entityDesc: SparqlEntitySchema,
                              path: TypedPath,
                              limit: Option[Int])
                             (implicit userContext: UserContext) extends Thread {
    private val queue = new LinkedBlockingQueue[PathValues](maxQueueSize)

    private var nextElement: Option[PathValues] = None

    def hasNext: Boolean = {
      if(nextElement.isDefined) {
        moreEntriesAvailable
      } else {
        nextElement = Option(queue.take())
        //Throw exceptions which occurred during querying
        moreEntriesAvailable
      }
    }

    private def moreEntriesAvailable: Boolean = nextElement match {
      case Some(e) => e != QueueEndMarker
      case _ => false
    }

    def next(): PathValues = {
      nextElement match {
        case Some(e) if e != QueueEndMarker =>
          nextElement = None
          e
        case _ =>
          queue.take()
      }
    }

    override def run(): Unit = {
      try {
        //Query for all entities
        queryPath().bindings.use { bindings =>
          parseResults(bindings)
        }
      }
      catch {
        case ex: Throwable =>
          queue.put(ExceptionPathValues(ex))
      }
    }

    private def queryPath()(implicit userContext: UserContext): SparqlResults = {
      val sparqlQuery = ParallelEntityRetriever.pathQuery(entityDesc.variable, entityDesc.restrictions, path.asUntypedPath,
        useDistinct = true, graphUri = graphUri, useOrderBy = useOrderBy, varPrefix = varPrefix, useOptional = true)

      endpoint.select(sparqlQuery, limit.getOrElse(Int.MaxValue))
    }

    private val QUEUE_OFFER_TIMEOUT = 3600 // 1 hour, just a high number
    private def queueElement(pathValues: PathValues): Boolean = queue.offer(pathValues, QUEUE_OFFER_TIMEOUT, TimeUnit.SECONDS)
    private val isSpecialLangPath = specialPaths.isLangSpecialPath(path)
    private val isSpecialTextPath = specialPaths.isTextSpecialPath(path)
    private val uriRequested = path.valueType == ValueType.URI

    private def parseResults(sparqlResults: Iterator[Map[String, RdfNode]]): Unit = {
      var currentSubject: Option[String] = None
      var currentValues: Seq[String] = Seq.empty
      def addCurrentValue(value: String): Unit = {
        currentValues = currentValues :+ value
      }
      val nonEmptyResults = sparqlResults.nonEmpty

      for (result <- sparqlResults) {
        if (canceled) {
          return
        }

        //Check if we are still reading values for the current subject
        val subject = EntityRetriever.extractSubject(result, entityDesc.variable)

        if (currentSubject.isEmpty) {
          currentSubject = subject
        } else if (subject.isDefined && subject != currentSubject) {
          queueElement(ExistingPathValues(currentSubject.get, currentValues))

          currentSubject = subject
          currentValues = Seq.empty
        }

        if (currentSubject.isDefined) {
          addValues(addCurrentValue, result)
        }
      }

      for (s <- currentSubject if nonEmptyResults) {
        queueElement(ExistingPathValues(s, currentValues))
      }
      queueElement(QueueEndMarker)
    }

    // Adds the requested values via the given add function.
    private def addValues(addCurrentValue: String => Unit,
                          result: Map[String, RdfNode]): Unit = {
      EntityRetriever.extractPathValue(
        result.get(entityDesc.variable),
        result.get(varPrefix + "0"),
        uriRequested = uriRequested,
        isLangSpecialPath = isSpecialLangPath,
        isSpecialPathOnly = path.size == 1 && (isSpecialTextPath || isSpecialLangPath)
      ) foreach(addCurrentValue)
    }
  }
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
                varPrefix: String,
                useOptional: Boolean): String = {
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
    sparql append SparqlPathBuilder(path :: Nil, "?" + subjectVar, "?" + varPrefix, useOptional = useOptional)

    sparql append "}" // END WHERE

    if (useOrderBy) {
      sparql append " ORDER BY " + "?" + subjectVar
    }
    sparql.toString
  }

  /** Returns the entity URIs for a specific SPARQL restriction.
    *
    * @param subjectVar  The variable name of the subject
    * @param restriction A SPARQL restriction defined on the subject. Must have the same variable name as the subjectVar
    * @param graphUri    An optional graph URI
    * @param useOrderBy  Should the results be ordered by the subjectVar
    */
  def entityUrisQuery(subjectVar: String,
                      restriction: SparqlRestriction,
                      graphUri: Option[String],
                      useOrderBy: Boolean): String = {
    val varPrefix = "internal__vars"
    //Select
    val sparql = new StringBuilder
    sparql append "SELECT DISTINCT "

    sparql append "?" + subjectVar + " "

    //Graph
    for (graph <- graphUri if !graph.isEmpty) sparql append "FROM <" + graph + ">\n"

    //Body
    sparql append "WHERE {\n"

    if (restriction.toSparql.isEmpty) {
      sparql append "?" + subjectVar + " ?" + varPrefix + "_p ?" + varPrefix + "_o .\n"
    } else {
      sparql append restriction.toSparql + "\n"
    }

    sparql append "}" // END WHERE

    if (useOrderBy) {
      sparql append " ORDER BY " + "?" + subjectVar
    }
    sparql.toString
  }

  sealed trait PathValues {
    def uri: String
    def values: Seq[String]
    def failure: Option[Throwable]
  }
  /** Actual values. */
  case class ExistingPathValues(uri: String, values: Seq[String]) extends PathValues {
    override def failure: Option[Throwable] = None
  }
  /** Object that marks the end of the queue. */
  object QueueEndMarker extends PathValues {
    val uri: String = ""
    val values: Seq[String] = Seq.empty
    val failure: Option[Throwable] = None
  }
  /** Failure case */
  case class ExceptionPathValues(exception: Throwable) extends PathValues {
    override def uri: String = throw exception
    override def values: Seq[String] = throw exception

    override def failure: Option[Throwable] = Some(exception)
  }
}
