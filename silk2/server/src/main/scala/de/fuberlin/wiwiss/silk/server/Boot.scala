package bootstrap.liftweb

import net.liftweb.http.LiftRules
import de.fuberlin.wiwiss.silk.server.RestApi

/**
  * A class that's instantiated early and run.  It allows the application
  * to modify lift's environment
  */
class Boot
{
    def boot
    {
        LiftRules.dispatch.prepend(RestApi.dispatch)
    }
}
