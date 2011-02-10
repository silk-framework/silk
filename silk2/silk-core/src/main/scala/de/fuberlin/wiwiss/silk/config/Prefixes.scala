package de.fuberlin.wiwiss.silk.config

import xml.Node

/**
 * Holds namespace prefixes.
 */
class Prefixes(private val map : Map[String, String])
{
  override def toString = "Prefixes(" + map.toString + ")"

  def toXML =
  {
    <Prefixes>
    {
      for((key, value) <- map) yield
      {
        <Prefix id={key} namespace={value} />
      }
    }
    </Prefixes>
  }
}

object Prefixes
{
  val empty = new Prefixes(Map.empty)

  implicit def fromMap(map : Map[String, String]) = new Prefixes(map)

  implicit def toMap(prefixes : Prefixes) = prefixes.map

  def apply(map : Map[String, String]) = new Prefixes(map)

  def fromXML(xml : Node) =
  {
    new Prefixes((xml \ "Prefix").map(n => (n \ "@id" text, n \ "@namespace" text)).toMap)
  }
}