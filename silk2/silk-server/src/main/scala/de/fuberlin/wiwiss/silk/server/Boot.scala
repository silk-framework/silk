package bootstrap.liftweb

import net.liftweb.http.LiftRules
import de.fuberlin.wiwiss.silk.server.view.RestApi
import de.fuberlin.wiwiss.silk.server.model.Server
import java.io.File
import de.fuberlin.wiwiss.silk.config.ConfigReader

/**
  * A class that's instantiated early and run.  It allows the application
  * to modify lift's environment
  */
class Boot
{
    def boot
    {
        Server.start()

        LiftRules.addToPackages("de.fuberlin.wiwiss.silk.server.view")
        LiftRules.dispatch.prepend(RestApi.dispatch)
    }
}
