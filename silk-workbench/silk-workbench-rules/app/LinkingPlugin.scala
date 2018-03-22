import org.silkframework.rule.LinkSpec
import plugins.WorkbenchPlugin.{Tab, TaskActions}
import plugins.{Context, WorkbenchPlugin}
import controllers.rules.routes.Assets
import org.silkframework.config.Prefixes

/**
 * The linking Workbench plugin.
 */
case class LinkingPlugin() extends WorkbenchPlugin {

  override def tasks = {
    Seq(LinkingTaskActions)
  }

  override def tabs(context: Context[_]) = {
    var tabs = List[Tab]()
    if(context.task.data.isInstanceOf[LinkSpec]) {
      val p = context.project.name
      val t = context.task.id
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

  object LinkingTaskActions extends TaskActions[LinkSpec] {

    /** The name of the task type */
    override def name: String = "Linking Task"

    /** Path to the task icon */
    override def icon: String = Assets.at("img/arrow-join.png").url

    override def folderIcon: String = Assets.at("img/linking-folder.png").url

    /** The path to the dialog for creating a new task. */
    override def createDialog(project: String) =
      Some(s"linking/dialogs/newLinkingTask/$project")

    /** The path to the dialog for editing an existing task. */
    override def propertiesDialog(project: String, task: String) =
      Some(s"linking/dialogs/editLinkingTask/$project/$task")

    /** The path to redirect to when the task is opened. */
    override def open(project: String, task: String) =
      Some(s"linking/$project/$task/editor")
  }

}
