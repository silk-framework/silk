import org.silkframework.config.LinkSpecification
import plugins.WorkbenchPlugin.{Tab, TaskActions}
import plugins.{Context, WorkbenchPlugin}

/**
 * The linking Workbench plugin.
 */
case class LinkingPlugin() extends WorkbenchPlugin {

  override def routes = Map("rules" -> rules.Routes, "linking" -> linking.Routes)

  override def tasks = {
    Seq(LinkingTaskActions)
  }

  override def tabs(context: Context[_]) = {
    var tabs = List[Tab]()
    if(context.task.data.isInstanceOf[LinkSpecification]) {
      val p = context.project.name
      val t = context.task.name
      if (config.workbench.tabs.editor)
        tabs ::= Tab("Editor", s"linking/$p/$t/editor")
      if (config.workbench.tabs.generateLinks)
        tabs ::= Tab("Generate Links", s"linking/$p/$t/generateLinks")
      if (config.workbench.tabs.learn)
        tabs ::= Tab("Learn", s"linking/$p/$t/learnStart")
      if (config.workbench.tabs.referenceLinks)
        tabs ::= Tab("Reference Links", s"linking/$p/$t/referenceLinks")
    }
    tabs.reverse
  }

  object LinkingTaskActions extends TaskActions[LinkSpecification] {

    /** The name of the task type */
    override def name: String = "Linking Task"

    /** Path to the task icon */
    override def icon: String = "img/arrow-join.png"

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"linking/dialogs/newLinkingTask/$project")

    /** The path to the dialog for editing an existing task. */
    override def propertiesDialog(project: String, task: String) =
      Some(s"linking/dialogs/editLinkingTask/$project/$task")

    /** The path to redirect to when the task is opened. */
    override def open(project: String, task: String) =
      Some(s"linking/$project/$task/editor")

    /** The path to delete the task by sending a DELETE HTTP request. */
    override def delete(project: String, task: String) =
      Some(s"linking/tasks/$project/$task")

    /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
    override def properties(task: Any): Seq[(String, String)] = {
      val linkSpec = task.asInstanceOf[LinkSpecification]
      Seq(
        ("Source", linkSpec.dataSelections.source.inputId.toString),
        ("Target", linkSpec.dataSelections.target.inputId.toString),
        ("Source Type", linkSpec.dataSelections.source.typeUri.toString),
        ("Target Type", linkSpec.dataSelections.target.typeUri.toString),
        ("Source Restriction", linkSpec.dataSelections.source.restriction.toString),
        ("Target Restriction", linkSpec.dataSelections.target.restriction.toString)
      )
    }
  }

}
