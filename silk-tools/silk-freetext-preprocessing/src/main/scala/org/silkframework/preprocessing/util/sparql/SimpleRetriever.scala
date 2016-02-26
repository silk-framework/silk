package org.silkframework.preprocessing.util.sparql

/**
 * EntityRetriever which executes a single SPARQL query to retrieve entities and/or properties.
 */
case class SimpleRetriever(endpoint: SparqlEndpoint, pageSize: Int = 1000, graphUri: Option[String] = None) {

  private val varPrefix = "v"

  def retrieve():Traversable[Entity] = {
    var sparql = "SELECT DISTINCT "
    sparql += "?" + varPrefix + "_s "
    sparql += "\n"

    //Graph
    for (graph <- graphUri if !graph.isEmpty) sparql += "FROM <" + graph + ">\n"

    //Body
    sparql += "WHERE {\n"
    sparql += "?" + varPrefix + "_s ?" + varPrefix + "_p ?" + varPrefix + "_o "
    sparql += "}"

    val sparqlResults = endpoint.query(sparql)

    new EntityTraversable(sparqlResults, "v_s")

  }

  def retriveProperties(uri:String):Traversable[Property] = {
    var sparql = "SELECT DISTINCT "
    sparql += "?" + varPrefix + "_p ?" + varPrefix + "_o"
    sparql += "\n"

    //Graph
    for (graph <- graphUri if !graph.isEmpty) sparql += "FROM <" + graph + ">\n"

    //Body
    sparql += "WHERE {\n"
    sparql += "?" + varPrefix + "_s ?" + varPrefix + "_p ?" + varPrefix + "_o\n"
    sparql += "FILTER(str(?" + varPrefix + "_s)=\""+uri+"\") "
    sparql += "}"

    val sparqlResults = endpoint.query(sparql)

    new PropertyTraversable(sparqlResults, ("v_p","v_o"))
  }





  /**
   * Wraps a Traversable of SPARQL results and retrieves entities from them.
   */
  private class EntityTraversable(sparqlResults: Traversable[Map[String, Node]],  variable: String) extends Traversable[Entity] {
    override def foreach[U](f: Entity => U) {

      for (result <- sparqlResults;
           node <- result.get(variable)) {
          val uri = node match {
            case Resource(value) => value
            case BlankNode(value) => value
          }

          f(new Entity(uri, retriveProperties(uri)))

      }
    }
  }

  /**
   * Wraps a Traversable of SPARQL results and retrieves (property,value) pairs.
   */
  private class PropertyTraversable(sparqlResults: Traversable[Map[String, Node]],  variables: (String,String)) extends Traversable[Property] {
    override def foreach[U](f: Property => U) {

      for (
           result <- sparqlResults;
           pathNode <- result.get(variables._1);
           valueNode <- result.get(variables._2)) {
        val path = pathNode match {
          case Resource(v) => v
        }
        val value = valueNode match {
          case Resource(v) => v
          case Literal(v) => v
        }

        f(new Property(path,value))

      }
    }
  }


}
