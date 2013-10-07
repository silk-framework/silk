import play.api.GlobalSettings
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import java.util.logging.ConsoleHandler
import de.fuberlin.wiwiss.silk.workspace.FileUser
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.plugins.jena.JenaPlugins
import scala.concurrent.Future

object Global extends GlobalSettings {

  println(new java.io.File(".").getAbsolutePath)

  val user = new FileUser

  User.userManager = () => user

  Plugins.register()
  JenaPlugins.register()
  
  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful(InternalServerError(ex.getMessage))
  }

}