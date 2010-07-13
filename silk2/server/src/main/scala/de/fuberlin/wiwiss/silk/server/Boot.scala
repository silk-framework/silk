package bootstrap.liftweb

import net.liftweb.http.LiftRules
import de.fuberlin.wiwiss.silk.server.{Server, RestApi}

/**
  * A class that's instantiated early and run.  It allows the application
  * to modify lift's environment
  */
class Boot
{
    def boot
    {
        Server.init()

        LiftRules.dispatch.prepend(RestApi.dispatch)
    }
}
