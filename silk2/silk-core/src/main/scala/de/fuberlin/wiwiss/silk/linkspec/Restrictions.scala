package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.config.Prefixes
import xml.Node
import util.matching.Regex

class Restrictions private(restrictionsFull : String, restrictionsQualified : String)
{
  override def toString = restrictionsQualified

  def toSparql = restrictionsFull

  def toXML = <Restrictions>{restrictionsQualified}</Restrictions>
}

object Restrictions
{
  def empty = new Restrictions("", "")

  def fromXML(node : Node)(implicit prefixes : Prefixes) =
  {
    fromSparql(node \ "Restrictions" text)
  }

  def fromSparql(restrictions : String)(implicit prefixes : Prefixes) =
  {
    var restrictionsFull = restrictions
    var restrictionsQualified = restrictions
    for((id, namespace) <- prefixes.toSeq.sortBy(_._1.length).reverse)
    {
      restrictionsFull = restrictionsFull.replaceAll(id + ":" + "([^\\s\\{\\}\\.]+)", "<" + namespace + "$1>")
      restrictionsQualified = restrictionsQualified.replaceAll("<" + namespace + "([^>]+)>", id + ":" + "$1")
    }

    //Check if a prefix is missing
    val missingPrefixes = new Regex("[\\s\\{\\}][^<\\s\\{\\}]+:").findAllIn(restrictionsFull)
    if(!missingPrefixes.isEmpty)
    {
      throw new IllegalArgumentException("The following prefixes are not defined: " + missingPrefixes.mkString(","))
    }

    new Restrictions(restrictionsFull, restrictionsQualified)
  }
}