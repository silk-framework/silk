package controllers.rules

import org.silkframework.dataset.Dataset
import org.silkframework.workspace.User
import org.silkframework.workspace.activity.dataset.TypesCache
import play.api.mvc.{Action, Controller}

object Dialogs extends Controller {

  def restrictionDialog(projectName: String, sourceName: String, varName: String, restriction: String) = Action {
    val project = User().workspace.project(projectName)
    val typesCache = project.task[Dataset](sourceName).activity[TypesCache].value.typesByFrequency
    implicit val prefixes = project.config.prefixes

    Ok(views.html.dialogs.restrictionDialog(project, varName, restriction))
  }

}
