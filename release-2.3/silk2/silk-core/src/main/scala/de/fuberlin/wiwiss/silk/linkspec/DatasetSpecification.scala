package de.fuberlin.wiwiss.silk.linkspec

import xml.Node

case class DatasetSpecification(val sourceId : String, val variable : String, val restriction : String)

object DatasetSpecification
{
  def fromXML(node : Node) : DatasetSpecification =
  {
    new DatasetSpecification(
      node \ "@dataSource" text,
      node \ "@var" text,
      (node \ "RestrictTo").text.trim
    )
  }
}
