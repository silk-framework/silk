package de.fuberlin.wiwiss.silk.workbench.lift

import org.mortbay.jetty.Server
import net.liftweb.http.{LiftServlet, LiftFilter}
import org.mortbay.jetty.servlet._
import org.mortbay.jetty.webapp.{WebAppClassLoader, WebAppContext}
import org.mortbay.jetty.nio.SelectChannelConnector

object Main
{
  def main(args : Array[String])
  {
    val server = new Server(8080)

    val context = new WebAppContext()

    context.setContextPath("/")

        val webAppClassLoader = new WebAppClassLoader(Main.getClass.getClassLoader(),context);
        context.setClassLoader(webAppClassLoader);


    val protectionDomain = Main.getClass.getProtectionDomain()
   val location = protectionDomain.getCodeSource().getLocation().toExternalForm
    context.setWar(location)

    println("Location: " + location)

    context.setExtractWAR(true)

    server.setHandler(context)

//        val  connector = new SelectChannelConnector();
//        connector.setPort(8080);
//        connector.setMaxIdleTime(1000);
//
//    server.setConnectors(Array(connector))

//    context.addFilter(new FilterHolder(new LiftFilter()), "/*", 1)

//    val servlet = new DefaultServlet()
//    val servletHolder = new ServletHolder(servlet)
//    servletHolder.setInitParameter("useFileMappedBuffer", "false")
//    context.addServlet(servletHolder, "/")

    server.setHandler(context)

    server.start()
  }
}
