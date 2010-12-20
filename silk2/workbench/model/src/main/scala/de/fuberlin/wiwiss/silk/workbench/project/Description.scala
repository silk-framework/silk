package de.fuberlin.wiwiss.silk.workbench.project

import de.fuberlin.wiwiss.silk.util.sparql.RemoteSparqlEndpoint
import xml.Node
import java.net.URI

//TODO change endpoint to endpointUri?
class Description(val endpoint : RemoteSparqlEndpoint, val restriction : String)
{
  def toXML() : Node =
  {
    <Description>
      <Endpoint>{endpoint.uri}</Endpoint>
      <Restriction>{restriction}</Restriction>
    </Description>
  }
}

object Description
{
  def fromXML(xml : Node) : Description =
  {
    val endpoint = new RemoteSparqlEndpoint(new URI(xml \ "Endpoint" text))
    val restriction = xml \ "Restriction" text;
    new Description(endpoint, restriction)
  }
}
