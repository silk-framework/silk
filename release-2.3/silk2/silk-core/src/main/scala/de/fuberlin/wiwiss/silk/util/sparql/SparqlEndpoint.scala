package de.fuberlin.wiwiss.silk.util.sparql

/**
 * Represents a SPARQL endpoint and provides an interface to execute queries on it.
 */
trait SparqlEndpoint
{
  val prefixes : Map[String, String]

  def query(sparql : String, limit : Int = Integer.MAX_VALUE) : Traversable[Map[String, Node]]
}
