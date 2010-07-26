package de.fuberlin.wiwiss.silk.server.view

import net.liftweb.http._
import net.liftweb.common.{Empty, Full}
import de.fuberlin.wiwiss.silk.jena.RdfDataSource
import de.fuberlin.wiwiss.silk.server.model.Server
import java.io.ByteArrayInputStream
import de.fuberlin.wiwiss.silk.config.ConfigLoader

object RestApi
{
    def dispatch : LiftRules.DispatchPF =
    {
        case req @ Req(List("api", "generateLinks"), "", PostRequest) => () => generateLinks(req)
        case Req(List(_), "", _) => () => Empty
    }

    //TODO allow the specification of the input and output format
    private def generateLinks(req : Req) =
    {
        val input = new String(getLoad(req), "UTF-8")
        val format = req.param("format").getOrElse("RDF/XML")

        val response = Server.process(new RdfDataSource(Map("input" -> input, "format" -> format)))

        Full(PlainTextResponse(response, Nil, 200))
    }

    private def addConfiguration(name : String, data : Array[Byte]) =
    {
        val config = ConfigLoader.load(new ByteArrayInputStream(data))

        Server.addConfig(config, name)

        Full(PlainTextResponse("Added configuration", Nil, 200))
    }

    private def getLoad(req : Req) : Array[Byte] =
    {
        if(!req.uploadedFiles.isEmpty)
        {
            req.uploadedFiles.head.file
        }
        else if(!req.body.isEmpty)
        {
            req.body.open_!
        }
        else
        {
            Array.empty
        }
    }
}
