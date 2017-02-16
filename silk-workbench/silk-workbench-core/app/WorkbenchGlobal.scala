
import java.util.logging.{Level, Logger}

import models.JsonError
import org.silkframework.workspace.{ProjectNotFoundException, TaskNotFoundException}
import play.api.PlayException.ExceptionSource
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.api.{Application, GlobalSettings}
import plugins.WorkbenchPlugins

import scala.concurrent.{ExecutionException, Future}

trait WorkbenchGlobal extends GlobalSettings with Rendering with AcceptExtractors {

}
