package controllers.transform

import org.silkframework.config.TransformSpecification
import org.silkframework.dataset.Dataset
import org.silkframework.workspace.User
import org.silkframework.workspace.activity.dataset.TypesCache
import play.api.mvc.{Action, Controller}

object TransformDialogs extends Controller {

  def transformationTaskDialog(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)

    if(taskName.nonEmpty) {
      val task = project.task[TransformSpecification](taskName)

      val sourceDataset = project.task[Dataset](task.data.selection.datasetId)
      val sourceTypes = sourceDataset.activity[TypesCache].value.types

      Ok(views.html.dialogs.transformationTaskDialog(projectName, taskName, sourceTypes))
    } else {
      Ok(views.html.dialogs.transformationTaskDialog(projectName, taskName, Seq.empty))
    }
  }

}