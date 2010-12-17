package de.fuberlin.wiwiss.silk.util.sparql

import de.fuberlin.wiwiss.silk.instance.{Path, Instance, InstanceSpecification}

class ParallelInstanceRetriever(endpoint : SparqlEndpoint, pageSize : Int = 1000, graphUri : Option[String] = None)
{
  private val varPrefix = "v"

  /**
   * Retrieves instances with a given instance specification.
   *
   * @param instanceSpec The instance specification
   * @param instances The URIs of the instances to be retrieved. If empty, all instances will be retrieved.
   * @return The retrieved instances
   */
  def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String]) : Traversable[Instance] =
  {
    //if(instances.isEmpty)
    {
      retrieveAll(instanceSpec)
    }
    //        else
    //        {
    //            retrieveList(instances, instanceSpec)
    //        }
  }

  /**
   * Retrieves all instances with a given instance specification.
   *
   * @param instanceSpec The instance specification
   * @return The retrieved instances
   */
  def retrieveAll(instanceSpec : InstanceSpecification) : Traversable[Instance] =
  {
     val pathQueries = instanceSpec.paths.map(path => (path.id, queryPath(instanceSpec, path))).toMap

     new InstanceTraversable(pathQueries, instanceSpec, None)
  }

  private def queryPath(instanceSpec : InstanceSpecification, path : Path) =
  {
    //Prefixes
    var sparql = instanceSpec.prefixes.map{case (prefix, uri) => "PREFIX " + prefix + ": <" + uri + ">\n"}.mkString

    //Select
    sparql += "SELECT DISTINCT "
    sparql += "?" + instanceSpec.variable + " "
    sparql += instanceSpec.paths.map("?" + varPrefix + _.id).mkString(" ") + "\n"

    //Graph
    for(graph <- graphUri) sparql += "FROM <" + graph + ">\n"

    //Body
    sparql += "WHERE {\n"
    if(instanceSpec.restrictions.isEmpty && instanceSpec.paths.isEmpty)
    {
      sparql += "?" + instanceSpec.variable + " ?" + varPrefix + "_p ?" + varPrefix + "_o "
    }
    else
    {
      sparql += instanceSpec.restrictions + "\n"
      sparql += SparqlPathBuilder(path :: Nil, "?" + instanceSpec.variable, "?" + varPrefix)
    }
    sparql += "}"

    endpoint.query(sparql)
  }

  /**
   * Wraps a Traversable of SPARQL results and retrieves instances from them.
   */
  private class InstanceTraversable(sparqlResults : Map[Int, Traversable[Map[String, Node]]], instanceSpec : InstanceSpecification, subject : Option[String]) extends Traversable[Instance]
  {
    override def foreach[U](f : Instance => U) : Unit =
    {
//      //Remember current subject
//      var curSubject : Option[String] = subject
//
//      //Collect values of the current subject
//      val values = collection.mutable.HashMap[Int, Set[String]]()
//
//      while(true)

//      {
//        for((id, results) <- sparqlResults) yield
//        {
//
//        }
//      }
//
//      for(result <- sparqlResults)
//      {
//        //If the subject is unknown, find binding for subject variable
//        if(subject.isEmpty)
//        {
//          //Check if we are still reading values for the current subject
//          val resultSubject = result.get(instanceSpec.variable) match
//          {
//            case Some(Resource(value)) => Some(value)
//            case _ => None
//          }
//
//          if(resultSubject != curSubject)
//          {
//            for(curSubjectUri <- curSubject)
//            {
//              f(new Instance(instanceSpec.variable, curSubjectUri, values.toMap))
//            }
//
//            curSubject = resultSubject
//            values.clear()
//          }
//        }
//
//        //Find results for values for the current subject
//        if(curSubject.isDefined)
//        {
//          for((variable, node) <- result if variable.startsWith(varPrefix))
//          {
//            val id = variable.substring(varPrefix.length).toInt
//
//            val oldVarValues = values.get(id).getOrElse(Set())
//
//            values(id) = oldVarValues + node.value
//          }
//        }
//      }
//
//      for(curSubjectUri <- curSubject)
//      {
//        f(new Instance(instanceSpec.variable, curSubjectUri, values.toMap))
//      }
    }
  }

  private class PathTraversable(variable : String, sparqlResults : Traversable[Map[String, Node]]) extends Traversable[Traversable[String]]
  {
    override def foreach[U](f : Traversable[String] => U) : Unit =
    {
      var currentSubject : String = null
      var currentValues : List[String] = Nil

      for(result <- sparqlResults)
      {
        //Check if we are still reading values for the current subject
        val Resource(subject) = result(variable)

        if(currentSubject == null)
        {
          currentSubject = subject
        }

        if(subject != currentSubject)
        {
          f(currentValues)

          currentSubject = subject
          currentValues = Nil
        }
        else
        {
          currentValues ::= subject
        }
      }

      f(currentValues)
    }
  }
}