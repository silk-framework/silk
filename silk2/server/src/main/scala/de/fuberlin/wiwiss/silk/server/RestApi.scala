package de.fuberlin.wiwiss.silk.server

import net.liftweb.http._
import net.liftweb.common.{Empty, Full}
import de.fuberlin.wiwiss.silk.jena.RdfDataSource

object RestApi
{
    def dispatch : LiftRules.DispatchPF =
    {
        case req @ Req(List("api", "post"), "", PostRequest) => () => handlePost(req)
        case Req(List(_), "", _) => () => Empty
    }

    def handlePost(req : Req) =
    {
        val request = new String(req.body.open_!, "UTF-8")

        val response = Server.process(new RdfDataSource(Map("input" -> request, "format" -> "RDF/XML")))

        Full(PlainTextResponse(response, Nil, 200))
    }
}
