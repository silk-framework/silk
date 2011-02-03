package de.fuberlin.wiwiss.silk.linkspec

import xml.Node
import de.fuberlin.wiwiss.silk.util.Identifier

case class DatasetSpecification(sourceId : Identifier, variable : String, restriction : String)

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
