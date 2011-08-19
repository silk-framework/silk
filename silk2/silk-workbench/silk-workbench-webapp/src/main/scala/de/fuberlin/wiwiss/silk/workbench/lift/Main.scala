package de.fuberlin.wiwiss.silk.workbench.lift

import org.mortbay.jetty.Server
import org.mortbay.jetty.webapp.{WebAppClassLoader, WebAppContext}

object Main {
  def main(args : Array[String]) {
    val server = new Server(8080)

    val context = new WebAppContext()

    context.setContextPath("/")

    val webAppClassLoader = new WebAppClassLoader(Main.getClass.getClassLoader,context);
    context.setClassLoader(webAppClassLoader);

    val protectionDomain = Main.getClass.getProtectionDomain
    val location = protectionDomain.getCodeSource.getLocation.toExternalForm
    context.setWar(location)

    context.setExtractWAR(true)

    server.setHandler(context)

    server.setHandler(context)

    server.start()
  }
}
