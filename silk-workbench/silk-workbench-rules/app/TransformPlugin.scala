import org.silkframework.config.TransformSpecification
import plugins.WorkbenchPlugin.{Tab, TaskActions}
import plugins.{Context, WorkbenchPlugin}

case class TransformPlugin() extends WorkbenchPlugin {

  override def routes = Map("rules" -> rules.Routes, "transform" -> transform.Routes)

  override def tasks = {
    Seq(TransformTaskActions)
  }

  override def tabs(context: Context[_]) = {
    var tabs = List[Tab]()
    if(context.task.data.isInstanceOf[TransformSpecification]) {
      val p = context.project.name
      val t = context.task.name
      tabs ::= Tab("Editor", s"transform/$p/$t/editor")
      tabs ::= Tab("Evaluate", s"transform/$p/$t/evaluate")
      tabs ::= Tab("Execute", s"transform/$p/$t/execute")
    }
    tabs.reverse
  }

  object TransformTaskActions extends TaskActions[TransformSpecification] {

    /** The name of the task type */
    override def name: String = "Transform Task"

    /** Path to the task icon */
    override def icon: String = "img/arrow-skip.png"

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"transform/dialogs/newTransformTask/$project")

    /** The path to the dialog for editing an existing task. */
    override def propertiesDialog(project: String, task: String) =
      Some(s"transform/dialogs/editTransformTask/$project/$task")

    /** The path to redirect to when the task is opened. */
    override def open(project: String, task: String) =
      Some(s"transform/$project/$task/editor")

    /** The path to delete the task by sending a DELETE HTTP request. */
    override def delete(project: String, task: String) =
      Some(s"transform/tasks/$project/$task")

    /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
    override def properties(task: Any): Seq[(String, String)] = {
      val transformSpec = task.asInstanceOf[TransformSpecification]
      Seq(
        ("Source", transformSpec.selection.datasetId.toString),
        ("Type", transformSpec.selection.typeUri.toString),
        ("Restriction", transformSpec.selection.restriction.toString)
      )
    }
  }
}
