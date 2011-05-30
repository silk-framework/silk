package de.fuberlin.wiwiss.silk.config

import xml.Node

/**
 * Holds namespace prefixes.
 */
class Prefixes(val prefixMap : Map[String, String])
{
  override def toString = "Prefixes(" + prefixMap.toString + ")"

  /**
   * Combines two prefix objects.
   */
  def ++(prefixes : Prefixes) =
  {
    new Prefixes(prefixMap ++ prefixes.prefixMap)
  }

  /**
   * Resolves a qualified name to its full URI.
   *
   * @param qualifiedName The qualified name e.g. rdf:label
   * @return The full URI e.g. http://www.w3.org/1999/02/22-rdf-syntax-ns#label
   * @see shorten
   */
  def resolve(qualifiedName : String) = qualifiedName.split(":", 2) match
  {
    case Array(prefix, suffix) => prefixMap.get(prefix) match
    {
      case Some(resolvedPrefix) => resolvedPrefix + suffix
      case None => throw new IllegalArgumentException("Unknown prefix: " + prefix)
    }
    case _ => throw new IllegalArgumentException("No prefix found in " + qualifiedName)
  }

  /**
   * Tries to shorten a full URI.
   *
   * @param uri The full URI e.g. http://www.w3.org/1999/02/22-rdf-syntax-ns#label
   * @return The qualified name if a prefix was found e.g. rdf:label. The full URI otherwise.
   * @see resolve
   */
  def shorten(uri : String) : String =
  {
    for((id, namespace) <- prefixMap if uri.startsWith(namespace))
    {
      return id + ":" + uri.substring(namespace.length)
    }

    uri
  }

  def toXML =
  {
    <Prefixes>
    {
      for((key, value) <- prefixMap) yield
      {
        <Prefix id={key} namespace={value} />
      }
    }
    </Prefixes>
  }
  
  def toSparql =
  {
    var sparql = ""
    for ((key, value) <- prefixMap)
       {
         sparql += "PREFIX "+key+": <"+value +"> "
       }
    sparql
  }

}

object Prefixes
{
  val empty = new Prefixes(Map.empty)

  implicit def fromMap(map : Map[String, String]) = new Prefixes(map)

  implicit def toMap(prefixes : Prefixes) = prefixes.prefixMap

  def apply(map : Map[String, String]) = new Prefixes(map)

  def fromXML(xml : Node) =
  {
    new Prefixes((xml \ "Prefix").map(n => (n \ "@id" text, n \ "@namespace" text)).toMap)
  }
}