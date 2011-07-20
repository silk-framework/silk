package de.fuberlin.wiwiss.silk.util.sparql

import de.fuberlin.wiwiss.silk.instance.{Path, Instance, InstanceSpecification}
import collection.mutable.SynchronizedQueue
import java.util.logging.{Level, Logger}

/**
 * InstanceRetriever which executes multiple SPARQL queries (one for each property path) in parallel and merges the results into single instances.
 */
class ParallelInstanceRetriever(endpoint: SparqlEndpoint, pageSize: Int = 1000, graphUri: Option[String] = None, useOrderBy: Boolean = false) extends InstanceRetriever {
  private val varPrefix = "v"

  private val maxQueueSize = 1000

  private val logger = Logger.getLogger(classOf[ParallelInstanceRetriever].getName)

  private var failed = false

  /**
   * Retrieves instances with a given instance specification.
   *
   * @param instanceSpec The instance specification
   * @param instances The URIs of the instances to be retrieved. If empty, all instances will be retrieved.
   * @return The retrieved instances
   */
  override def retrieve(instanceSpec: InstanceSpecification, instances: Seq[String]): Traversable[Instance] = {
    new InstanceTraversable(instanceSpec, instances)
  }

  /**
   * Wraps a Traversable of SPARQL results and retrieves instances from them.
   */
  private class InstanceTraversable(instanceSpec: InstanceSpecification, instanceUris: Seq[String]) extends Traversable[Instance] {
    override def foreach[U](f: Instance => U) {
      var inconsistentOrder = false
      var counter = 0

      val pathRetrievers = for (path <- instanceSpec.paths) yield new PathRetriever(instanceUris, instanceSpec, path)

      pathRetrievers.foreach(_.start())

      try {
        while (pathRetrievers.forall(_.hasNext) && !inconsistentOrder) {
          val pathValues = for (pathRetriever <- pathRetrievers) yield pathRetriever.next()

          if (pathValues.tail.forall(_.uri == pathValues.head.uri)) {
            f(new Instance(pathValues.head.uri, pathValues.map(_.values).toIndexedSeq, instanceSpec))

            counter += 1
          }
          else {
            inconsistentOrder = true
            failed = true
          }
        }
      }
      catch {
        case ex: Exception => {
          logger.log(Level.WARNING, "Failed to execute query for '" + instanceSpec.restrictions + "'", ex)
          failed = true
        }
      }

      if (inconsistentOrder) {
        if (!useOrderBy) {
          logger.info("Querying endpoint '" + endpoint + "' without order-by failed. Using order-by.")
          val instanceRetriever = new ParallelInstanceRetriever(endpoint, pageSize, graphUri, true)
          val instances = instanceRetriever.retrieve(instanceSpec, instanceUris)
          instances.drop(counter).foreach(f)
        }
        else {
          logger.warning("Cannot execute queries in parallel on '" + endpoint + "' because the endpoint returned the results in different orders.")
          val simpleInstanceRetriever = new SimpleInstanceRetriever(endpoint, pageSize, graphUri)
          val instances = simpleInstanceRetriever.retrieve(instanceSpec, instanceUris)
          instances.drop(counter).foreach(f)
        }
      }
    }
  }

  private class PathRetriever(instanceUris: Seq[String], instanceSpec: InstanceSpecification, path: Path) extends Thread {
    private val queue = new SynchronizedQueue[PathValues]()

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

      queue.dequeue
    }

    override def run() {
      try {
        if (instanceUris.isEmpty) {
          //Query for all instances
          val sparqlResults = queryPath()
          parseResults(sparqlResults)
        }
        else {
          //Query for a list of instances
          for (instanceUri <- instanceUris) {
            val sparqlResults = queryPath(Some(instanceUri))
            parseResults(sparqlResults, Some(instanceUri))
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
        sparql += "?" + instanceSpec.variable + " "
      }
      sparql += "?" + varPrefix + "0\n"

      //Graph
      for (graph <- graphUri) sparql += "FROM <" + graph + ">\n"

      //Body
      sparql += "WHERE {\n"
      fixedSubject match {
        case Some(subjectUri) => {
          sparql += SparqlPathBuilder(path :: Nil, "<" + subjectUri + ">", "?" + varPrefix)
        }
        case None => {
          sparql += instanceSpec.restrictions.toSparql + "\n"
          sparql += SparqlPathBuilder(path :: Nil, "?" + instanceSpec.variable, "?" + varPrefix)
        }
      }
      sparql += "}"

      if (useOrderBy && fixedSubject.isEmpty) {
        sparql += " ORDER BY " + "?" + instanceSpec.variable
      }

      endpoint.query(sparql)
    }

    private def parseResults(sparqlResults: Traversable[Map[String, Node]], fixedSubject: Option[String] = None) {
      var currentSubject: Option[String] = fixedSubject
      var currentValues: Set[String] = Set.empty

      for (result <- sparqlResults) {
        if (failed) {
          return
        }

        if (!fixedSubject.isDefined) {
          //Check if we are still reading values for the current subject
          val subject = result.get(instanceSpec.variable) match {
            case Some(Resource(value)) => Some(value)
            case _ => None
          }

          if (currentSubject.isEmpty) {
            currentSubject = subject
          }
          else if (subject.isDefined && subject != currentSubject) {
            while (queue.size > maxQueueSize) {
              Thread.sleep(100)
            }

            queue.enqueue(PathValues(currentSubject.get, currentValues))

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

      for (s <- currentSubject) {
        queue.enqueue(PathValues(s, currentValues))
      }
    }
  }

  private case class PathValues(uri: String, values: Set[String])

}
