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
        case req @ Req(List("api", "generateLinks"), "", PostRequest) => () => generateLinks(getLoad(req))
        case Req(List(_), "", _) => () => Empty
    }

    private def generateLinks(data : Array[Byte]) =
    {
        val request = new String(data, "UTF-8")

        val response = Server.process(new RdfDataSource(Map("input" -> request, "format" -> "RDF/XML")))

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
