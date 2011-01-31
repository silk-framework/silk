package de.fuberlin.wiwiss.silk.config

import xml.Node

class Prefixes(private val map : Map[String, String])
{
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
  implicit def toMap(prefixes : Prefixes) = prefixes.map

  def fromXML(xml : Node) =
  {
    new Prefixes((xml \ "Prefix").map(n => (n \ "@id" text, n \ "@namespace" text)).toMap)
  }
}