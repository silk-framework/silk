package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.config.Prefixes
import xml.Node

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

  def fromSparql(restrictionsQualified : String)(implicit prefixes : Prefixes) =
  {
    var restrictionsFull = restrictionsQualified
    restrictionsFull = restrictionsFull.replaceAll("[^\\s]+:[^\\s]+", "<$0>")
    for((id, namespace) <- prefixes.toSeq.sortBy(_._1.length).reverse)
    {
      restrictionsFull = restrictionsFull.replace(id + ":", namespace)
    }

    new Restrictions(restrictionsFull, restrictionsQualified)
  }
}