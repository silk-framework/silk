package de.fuberlin.wiwiss.silk.util.sparql

import de.fuberlin.wiwiss.silk.instance.{Path, InstanceSpecification, Instance}

/**
 * InstanceRetriever which executes a single SPARQL query to retrieve the instances.
 */
class SimpleInstanceRetriever(endpoint: SparqlEndpoint, pageSize: Int = 1000, graphUri: Option[String] = None) extends InstanceRetriever {
  private val varPrefix = "v"

  /**
   * Retrieves instances with a given instance specification.
   *
   * @param instanceSpec The instance specification
   * @param instances The URIs of the instances to be retrieved. If empty, all instances will be retrieved.
   * @return The retrieved instances
   */
  override def retrieve(instanceSpec: InstanceSpecification, instances: Seq[String]): Traversable[Instance] = {
    if (instances.isEmpty) {
      retrieveAll(instanceSpec)
    } else {
      retrieveList(instances, instanceSpec)
    }
  }

  /**
   * Retrieves all instances with a given instance specification.
   *
   * @param instanceSpec The instance specification
   * @return The retrieved instances
   */
  private def retrieveAll(instanceSpec: InstanceSpecification): Traversable[Instance] = {
    //Select
    var sparql = "SELECT DISTINCT "
    sparql += "?" + instanceSpec.variable + " "
    for (i <- 0 until instanceSpec.paths.size) {
      sparql += "?" + varPrefix + i + " "
    }
    sparql += "\n"

    //Graph
    for (graph <- graphUri) sparql += "FROM <" + graph + ">\n"

    //Body
    sparql += "WHERE {\n"
    if (instanceSpec.restrictions.toSparql.isEmpty && instanceSpec.paths.isEmpty) {
      sparql += "?" + instanceSpec.variable + " ?" + varPrefix + "_p ?" + varPrefix + "_o "
    } else {
      sparql += instanceSpec.restrictions.toSparql + "\n"
      sparql += SparqlPathBuilder(instanceSpec.paths, "?" + instanceSpec.variable, "?" + varPrefix)
    }
    sparql += "}"

    val sparqlResults = endpoint.query(sparql)

    new InstanceTraversable(sparqlResults, instanceSpec, None)
  }

  /**
   * Retrieves a list of instances.
   *
   * @param instanceUris The URIs of the instances
   * @param instanceSpec The instance specification
   * @return A sequence of the retrieved instances. If a instance is not in the store, it wont be included in the returned sequence.
   */
  private def retrieveList(instanceUris: Seq[String], instanceSpec: InstanceSpecification): Seq[Instance] = {
    instanceUris.view.flatMap(instanceUri => retrieveInstance(instanceUri, instanceSpec))
  }

  /**
   * Retrieves a single instance.
   *
   * @param instanceUri The URI of the instance
   * @param instanceSpec The instance specification
   * @return Some(instance), if a instance with the given uri is in the Store
   *         None, if no instance with the given uri is in the Store
   */
  def retrieveInstance(instanceUri: String, instanceSpec: InstanceSpecification): Option[Instance] = {
    //Query only one path at once and combine the result into one
    val sparqlResults = {
      for ((path, pathIndex) <- instanceSpec.paths.zipWithIndex;
           results <- retrievePaths(instanceUri, Seq(path))) yield {
        results map {
          case (variable, node) => (varPrefix + pathIndex, node)
        }
      }
    }

    new InstanceTraversable(sparqlResults, instanceSpec, Some(instanceUri)).headOption
  }

  private def retrievePaths(instanceUri: String, paths: Seq[Path]) = {
    //Select
    var sparql = "SELECT DISTINCT "
    for (i <- 0 until paths.size) {
      sparql += "?" + varPrefix + i + " "
    }
    sparql += "\n"

    //Graph
    for (graph <- graphUri) sparql += "FROM <" + graph + ">\n"

    //Body
    sparql += "WHERE {\n"
    sparql += SparqlPathBuilder(paths, "<" + instanceUri + ">", "?" + varPrefix)
    sparql += "}"

    endpoint.query(sparql)
  }

  /**
   * Wraps a Traversable of SPARQL results and retrieves instances from them.
   */
  private class InstanceTraversable(sparqlResults: Traversable[Map[String, Node]], instanceSpec: InstanceSpecification, subject: Option[String]) extends Traversable[Instance] {
    override def foreach[U](f: Instance => U) {
      //Remember current subject
      var curSubject: Option[String] = subject

      //Collect values of the current subject
      var values = Array.fill(instanceSpec.paths.size)(Set[String]())

      for (result <- sparqlResults) {
        //If the subject is unknown, find binding for subject variable
        if (subject.isEmpty) {
          //Check if we are still reading values for the current subject
          val resultSubject = result.get(instanceSpec.variable) match {
            case Some(Resource(value)) => Some(value)
            case _ => None
          }

          if (resultSubject != curSubject) {
            for (curSubjectUri <- curSubject) {
              f(new Instance(curSubjectUri, values, instanceSpec))
            }

            curSubject = resultSubject
            values = Array.fill(instanceSpec.paths.size)(Set[String]())
          }
        }

        //Find results for values for the current subject
        if (curSubject.isDefined) {
          for ((variable, node) <- result if variable.startsWith(varPrefix)) {
            val id = variable.substring(varPrefix.length).toInt

            values(id) += node.value
          }
        }
      }

      for (curSubjectUri <- curSubject) {
        f(new Instance(curSubjectUri, values, instanceSpec))
      }
    }
  }

}