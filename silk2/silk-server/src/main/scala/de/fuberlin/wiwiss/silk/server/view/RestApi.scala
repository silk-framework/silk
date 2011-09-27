package de.fuberlin.wiwiss.silk.server.view

import net.liftweb.http._
import net.liftweb.common.{Empty, Full}
import de.fuberlin.wiwiss.silk.plugins.jena.RdfDataSource
import de.fuberlin.wiwiss.silk.server.model.Server

object RestApi {
  def dispatch : LiftRules.DispatchPF = {
    case req @ Req(List("api", "process"), "", PostRequest) => () => generateLinks(req)
    case Req(List(_), "", _) => () => Empty
  }

  private def generateLinks(req : Req) = {
    val input = new String(getLoad(req), "UTF-8")
    val format = req.param("format").getOrElse("RDF/XML")

    val response = Server.process(new RdfDataSource(input, format))

    Full(PlainTextResponse(response, Nil, 200))
  }

  private def getLoad(req : Req) : Array[Byte] ={
    if(!req.uploadedFiles.isEmpty)
      req.uploadedFiles.head.file
    else if(!req.body.isEmpty)
      req.body.open_!
    else
      Array.empty
  }
}
