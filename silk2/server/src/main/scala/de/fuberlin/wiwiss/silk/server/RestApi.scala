package de.fuberlin.wiwiss.silk.server

import net.liftweb.http._
import net.liftweb.common.{Empty, Full}

object RestApi
{
    def dispatch : LiftRules.DispatchPF =
    {
        case Req(List("api", "post"), "", GetRequest) => () => Full(PlainTextResponse("RESPONSE", Nil, 200))
        case Req(List(_), "", _) => () => Empty
    }
}
