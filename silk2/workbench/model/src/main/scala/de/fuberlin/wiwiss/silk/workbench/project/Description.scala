package de.fuberlin.wiwiss.silk.workbench.project

import xml.Node
import java.net.URI

class Description(val endpointUri : URI, val restriction : String)
{
  def toXML() : Node =
  {
    <Description>
      <Endpoint>{endpointUri}</Endpoint>
      <Restriction>{restriction}</Restriction>
    </Description>
  }
}

object Description
{
  def fromXML(xml : Node) : Description =
  {
    val endpoint = new URI(xml \ "Endpoint" text)
    val restriction = xml \ "Restriction" text;
    new Description(endpoint, restriction)
  }
}
