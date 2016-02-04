package controllers.linking

import org.silkframework.config.LinkSpecification
import org.silkframework.dataset.Dataset
import org.silkframework.util.DPair
import org.silkframework.workspace.User
import org.silkframework.workspace.activity.dataset.TypesCache
import play.api.mvc.{Action, Controller}

object LinkingDialogs extends Controller {

  def linkingTaskDialog(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    if(taskName.nonEmpty) {
      val task = project.task[LinkSpecification](taskName)

      val sourceDataset = project.task[Dataset](task.data.dataSelections.source.datasetId)
      val targetDataset = project.task[Dataset](task.data.dataSelections.target.datasetId)

      val sourceTypes = sourceDataset.activity[TypesCache].value.types
      val targetTypes = targetDataset.activity[TypesCache].value.types

      Ok(views.html.dialogs.linkingTaskDialog(projectName, taskName, DPair(sourceTypes, targetTypes)))
    } else {
      Ok(views.html.dialogs.linkingTaskDialog(projectName, taskName, DPair(Seq.empty, Seq.empty)))
    }
  }

}