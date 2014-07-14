package plugins

import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformTask

case class TransformPlugin() extends WorkbenchPlugin {

  def tabs(context: Context[ModuleTask]) = {
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
}
