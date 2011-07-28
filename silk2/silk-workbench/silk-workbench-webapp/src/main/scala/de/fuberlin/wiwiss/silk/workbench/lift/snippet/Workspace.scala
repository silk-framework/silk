package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.util.Helpers._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST
import net.liftweb.http.js.JsCmds.Script
import xml.NodeSeq
import net.liftweb.http.js.{JsCmd, JsCmds}
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS
import net.liftweb.http.SHtml
import net.liftweb.json.Printer.pretty
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.output.LinkWriter
import net.liftweb.json.JsonAST.{JObject, JArray, JValue}

/**
 * Workspace snippet.
 *
 * Injects the following functions:
 *
 * def createProject()
 * def removeProject(projectName)
 * def importProject()
 * def exportProject(projectName)
 * def editPrefixes(projectName)
 * def addLinkSpecification(projectName)
 *
 * def createSourceTask(projectName)
 * def editSourceTask(projectName, taskName)
 * def removeSourceTask(projectName, taskName)
 *
 * def createLinkingTask(projectName)
 * def editLinkingTask(projectName, taskName)
 * def openLinkingTask(projectName, taskName)
 * def removeLinkingTask(projectName, taskName)
 *
 * def createOutput(projectName)
 * def editOutput(projectName, taskName)
 * def removeOutput(projectName, taskName)
 *
 * def closeTask()
 *
 * Whenever the workspace changes, the following function will be called:
 *
 * def updateWorkspace(
 *     workspace : JSON containing the workspace contents
 * )
 *
 */
class Workspace {
  def content(xhtml: NodeSeq): NodeSeq = {
    User().closeTask()

    bind("entry", xhtml,
         "injectedJavascript" -> (Script(Workspace.javasScriptFunctions)))
  }
}

object Workspace {
  def javasScriptFunctions = {
    updateCmd &
    createProjectFunction &
    removeProjectFunction &
    importProjectFunction &
    exportProjectFunction &
    editPrefixesFunction &
    createSourceTaskFunction &
    editSourceTaskFunction &
    injectFunction("removeSourceTask", removeSourceTask _) &
    createLinkingTaskFunction &
    editLinkingTaskFunction &
    openLinkingTaskFunction &
    injectFunction("removeLinkingTask", removeLinkingTask _) &
    createOutputFunction &
    editOutputFunction &
    injectFunction("removeOutput", removeOutput _) &
    addLinkSpecificationFunction &
    hideLoadingDialogCmd &
    closeTaskFunction
  }

  /**
   * JS Command which updates the workspace view.
   */
  def updateCmd: JsCmd = {
    JsRaw("var workspaceVar = " + pretty(JsonAST.render(workspaceJson)) + "; updateWorkspace(workspaceVar);").cmd
  }

  /**
   * JS Command which defines the createProject function
   */
  private def createProjectFunction: JsCmd = {
    val ajaxCall = SHtml.ajaxInvoke(CreateProjectDialog.openCmd _)._2.cmd
    CreateProjectDialog.initCmd & JsCmds.Function("createProject", Nil, ajaxCall)
  }

  /**
   * JS Command which defines the removeProject function
   */
  private def removeProjectFunction: JsCmd = {
    def callback(projectName: String): JsCmd = {
      User().workspace.removeProject(projectName)
      updateCmd
    }

    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName"), callback _)._2.cmd
    JsCmds.Function("removeProject", "projectName" :: Nil, ajaxCall)
  }

  /**
   * JS Command which defines the importProject function
   */
  private def importProjectFunction: JsCmd = {
    val ajaxCall = SHtml.ajaxInvoke(ImportProjectDialog.openCmd _)._2.cmd
    ImportProjectDialog.initCmd & JsCmds.Function("importProject", Nil, ajaxCall)
  }

  /**
   * JS Command which defines the exportProject function
   */
  private def exportProjectFunction: JsCmd = {
    def callback(projectName: String): JsCmd = {
      User().project = User().workspace.project(projectName)

      JS.Redirect("project.xml")
    }

    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName"), callback _)._2.cmd
    JsCmds.Function("exportProject", "projectName" :: Nil, ajaxCall)
  }

  /**
   * JS Command which defines the editPrefixes function
   */
  private def editPrefixesFunction: JsCmd = {
    def callback(projectName: String): JsCmd = {
      User().project = User().workspace.project(projectName)
      EditPrefixesDialog.openCmd
    }

    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName"), callback)._2.cmd
    EditPrefixesDialog.initCmd & JsCmds.Function("editPrefixes", "projectName" :: Nil, ajaxCall)
  }

  /**
   * JS Command which defines the createDataSourceTask function
   */
  private def createSourceTaskFunction: JsCmd = {
    def callback(projectName: String): JsCmd = {
      User().project = User().workspace.project(projectName)
      SourceTaskDialog.openCmd
    }

    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName"), callback _)._2.cmd
    val openSourceTaskDialog = JsCmds.Function("createSourceTask", "projectName" :: Nil, ajaxCall)
    SourceTaskDialog.initCmd & openSourceTaskDialog
  }

  /**
   * JS Command which defines the editSourceTask function
   */
  private def editSourceTaskFunction: JsCmd = {
    def callback(args: String): JsCmd = {
      val Array(projectName, taskName) = args.split(',')

      User().project = User().workspace.project(projectName)
      User().task = User().project.sourceModule.task(taskName)

      SourceTaskDialog.openCmd
    }

    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName + ',' + taskName"), callback _)._2.cmd
    JsCmds.Function("editSourceTask", "projectName" :: "taskName" :: Nil, ajaxCall)
  }

  /**
   * Removes a source task from the workspace.
   */
  private def removeSourceTask(projectName: String, taskName: String) {
    User().workspace.project(projectName).sourceModule.remove(taskName)
  }

  /**
   * JS Command which defines the createLinkingTask function
   */
  private def createLinkingTaskFunction: JsCmd = {
    def callback(projectName: String): JsCmd = {
      User().project = User().workspace.project(projectName)

      CreateLinkingTaskDialog.openCmd
    }

    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName"), callback _)._2.cmd
    val openLinkingTaskDialog = JsCmds.Function("createLinkingTask", "projectName" :: Nil, ajaxCall)
    CreateLinkingTaskDialog.initCmd & openLinkingTaskDialog
  }

  /**
   * JS Command which defines the editLinkingTask function
   */
  private def editLinkingTaskFunction : JsCmd = {
    def callback(args : String) : JsCmd = { //JS.Try("edit linking task")
      val Array(projectName, taskName) = args.split(',')

      User().project = User().workspace.project(projectName)
      User().task = User().project.linkingModule.task(taskName)

      EditLinkingTaskDialog.openCmd
    }

    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName + ',' + taskName"), callback _)._2.cmd
    EditLinkingTaskDialog.initCmd & JsCmds.Function("editLinkingTask", "projectName" :: "taskName" :: Nil, ajaxCall)
  }

  /*
   * JS Command which defines the openLinkingTask function
   */
  private def openLinkingTaskFunction: JsCmd = {
    def openLinkingTask(args: String): JsCmd = {
      val Array(projectName, taskName) = args.split(',')

      try {
        User().project = User().workspace.project(projectName)
        User().task = User().project.linkingModule.task(taskName)

        JS.Redirect("editor.html")
      } catch {
        case ex: Exception => JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName + ',' + taskName"), openLinkingTask _)._2.cmd
    JsCmds.Function("openLinkingTask", "projectName" :: "taskName" :: Nil, ajaxCall)
  }

  /**
   * Removes a linking task from the workspace.
   */
  private def removeLinkingTask(projectName: String, taskName: String) {
    User().workspace.project(projectName).linkingModule.remove(taskName)
  }

  /**
   * JS Command which defines the createOutput function
   */
  private def createOutputFunction: JsCmd = {
    def callback(projectName: String): JsCmd = {
      User().project = User().workspace.project(projectName)
      OutputDialog.Commands.open
    }

    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName"), callback _)._2.cmd
    val openOutputDialog = JsCmds.Function("createOutput", "projectName" :: Nil, ajaxCall)
    OutputDialog.Commands.init & openOutputDialog
  }

  /**
   * JS Command which defines the editOutput function
   */
  private def editOutputFunction: JsCmd = {
    def callback(args: String): JsCmd = {
      val Array(projectName, taskName) = args.split(',')

      User().project = User().workspace.project(projectName)
      User().task = User().project.outputModule.task(taskName)

      OutputDialog.Commands.open
    }

    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName + ',' + taskName"), callback _)._2.cmd
    JsCmds.Function("editOutput", "projectName" :: "taskName" :: Nil, ajaxCall)
  }

  /**
   * Removes an output from the workspace.
   */
  private def removeOutput(projectName: String, taskName: String) {
    User().workspace.project(projectName).outputModule.remove(taskName)
  }

  /**
   * Adds a link specification to the project.
   */
  private def addLinkSpecificationFunction = {
    def addLinkSpecification(projectName: String) = {
      User().project = User().workspace.project(projectName)
      AddLinkSpecificationDialog.openCmd
    }

    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName"), addLinkSpecification _)._2.cmd
    AddLinkSpecificationDialog.initCmd & JsCmds.Function("addLinkSpecification", "projectName" :: Nil, ajaxCall)
  }

  /**
   * JS Command which hides the loading dialog.
   */
  def hideLoadingDialogCmd: JsCmd = {
    JsRaw("loadingHide();").cmd
  }

  /**
   * JS Command which defines the closeTask function
   */
  private def closeTaskFunction(): JsCmd = {
    def closeTask() = {
      User().closeTask()
      JS.Empty
    }

    JsCmds.Function("closeTask", Nil, SHtml.ajaxInvoke(closeTask)._2.cmd)
  }

  /*
   * Injects a Javascript function.
   */
  private def injectFunction(name: String, func: (String, String) => Unit): JsCmd = {
    //Callback which executes the provided function
    def callback(args: String): JsCmd = {
      try {
        val Array(projectName, taskName) = args.split(',')

        func(projectName, taskName)

        updateCmd
      } catch {
        case ex: Exception => JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    //Ajax Call which executes the callback
    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName + ',' + taskName"), callback _)._2.cmd
    //JavaScript function definition
    JsCmds.Function(name, "projectName" :: "taskName" :: Nil, ajaxCall)
  }

  /**
   * Generates a JSON which contains the workspace contents.
   */
  private def workspaceJson: JValue = {
    // TODO - Nested 'yield's seem to cause the (strange) compiler error: 'xxx is not an enclosing class'
    var projectList: List[JValue] = List()

    for (project <- User().workspace.projects.toSeq.sortBy(n => (n.name.toString.toLowerCase))) {
      implicit val prefixes = project.config.prefixes

      val sources: JArray = for (task <- project.sourceModule.tasks.toSeq.sortBy(n => (n.name.toString.toLowerCase))) yield {
        task.source.dataSource match {
          case DataSource(_, params) => {
            ("name" -> task.name.toString) ~
            ("params" -> paramsToJson(params))
          }
        }
      }

      val linkingTasks: JArray = for (task <- project.linkingModule.tasks.toSeq.sortBy(n => (n.name.toString.toLowerCase))) yield {
        ("name" -> task.name.toString) ~
        ("source" -> task.linkSpec.datasets.source.sourceId.toString) ~
        ("target" -> task.linkSpec.datasets.target.sourceId.toString) ~
        ("sourceDataset" -> task.linkSpec.datasets.source.restriction.toString) ~
        ("targetDataset" -> task.linkSpec.datasets.target.restriction.toString) ~
        ("linkType" -> task.linkSpec.linkType.toTurtle)
      }

      val outputs: JArray = for (task <- project.outputModule.tasks.toSeq.sortBy(n => (n.name.toString.toLowerCase))) yield {
        task.output.writer match {
          case LinkWriter(_, params) => {
            ("name" -> task.name.toString) ~
            ("params" -> paramsToJson(params))
          }
        }
      }

      val proj: JObject = {
        ("name" -> project.name.toString) ~
        ("dataSource" -> sources) ~
        ("linkingTask" -> linkingTasks) ~
        ("output" -> outputs)
      }

      projectList :+= proj
    }

    val projects = ("project" -> JArray(projectList))
    val activeProject = ("activeProject" -> (if (User().projectOpen) User().project.name.toString else ""))
    val activeTask = ("activeTask" -> (if (User().taskOpen) User().task.name.toString else ""))
    val activeTaskType = ("activeTaskType" -> (if (User().taskOpen) User().task.getClass.getSimpleName else ""))

    ("workspace" -> projects ~ activeProject ~ activeTask ~ activeTaskType)
  }

  private def paramsToJson(params: Map[String, String]): JArray = {
    for ((key, value) <- params.toSeq) yield {
      ("key" -> key) ~
      ("value" -> value)
    }
  }
}
