package de.fuberlin.wiwiss.silk.workbench.lift

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext

/**
 * Starts the Workbench.
 */
object Main {
  def main(args : Array[String]) {
    val server = new Server(8080)

    val webapp = new WebAppContext();
    webapp.setContextPath("/");
    val protectionDomain = Main.getClass.getProtectionDomain
    val location = protectionDomain.getCodeSource.getLocation.toExternalForm
    webapp.setWar(location);
    server.setHandler(webapp);

    server.start()
  }
}
