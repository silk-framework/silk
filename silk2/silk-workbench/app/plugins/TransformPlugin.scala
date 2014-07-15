package plugins

import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformTask

case class TransformPlugin() extends WorkbenchPlugin {

  override def tasks = {
    Seq(TransformTaskActions)
  }

  override def tabs(context: Context[ModuleTask]) = {
    var tabs = List[Tab]()
    if(context.task.isInstanceOf[TransformTask]) {
      val p = context.project.name
      val t = context.task.name
      tabs ::= Tab("Editor", s"transform/$p/$t/editor")
      tabs ::= Tab("Evaluate", s"transform/$p/$t/evaluate")
      tabs ::= Tab("Execute", s"transform/$p/$t/execute")
    }
    tabs.reverse
  }

  object TransformTaskActions extends TaskActions[TransformTask] {

    /** The name of the task type */
    override def task: String = "Transform Task"

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      s"workspace/dialogs/newTransformTask/$project"

    /** The path to the dialog for editing an existing task. */
    override def editDialog(project: String, task: String) =
      s"workspace/dialogs/editTransformTask/$project/$task"

    /** The path to redirect to when the task is opened. */
    override def open(project: String, task: String): String =
      s"transform/$project/$task/editor"

    /** The path to delete the task by sending a DELETE HTTP request. */
    override def delete(project: String, task: String): String =
      s"transform/tasks/$project/$task"
  }
}
