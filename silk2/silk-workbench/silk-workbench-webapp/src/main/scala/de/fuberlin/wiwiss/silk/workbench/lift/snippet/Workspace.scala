package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST
import net.liftweb.http.js.JsCmds.{Script, OnLoad}
import xml.NodeSeq
import net.liftweb.http.js.{JsCmd, JsCmds}
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.json.JsonAST.{JObject, JArray, JValue}

/**
 * Workspace snippet.
 *
 * Injects the following functions:
 *
 * def createLinkingTask(
 *     projectName : The name of the project
 * )
 *
 * def openLinkingTask(
 *     projectName : The name of the project,
 *     taskName : The name of the task to be removed,
 * )
 *
 * def removeLinkingTask(
 *     projectName : The name of the project,
 *     taskName : The name of the task to be removed,
 * )
 *
 * Whenever the workspace changes, the following function will be called:
 *
 * def updateWorkspace(
 *     workspace : JSON containing the workspace contents
 * )
 *
 */
class Workspace
{
  def content(xhtml : NodeSeq) : NodeSeq =
  {
    bind("entry", xhtml,
         "injectedJavascript" -> (Script(Workspace.javasScriptFunctions)))
  }
}

object Workspace
{
  def javasScriptFunctions =
  {
    updateWorkspaceCmd &
    createLinkingTaskFunction &
    injectFunction("openLinkingTask", openLinkingTask _, true) &
    injectFunction("removeLinkingTask", removeLinkingTask _)
  }

  /**
   * JS Command which updates the workspace view.
   */
  def updateWorkspaceCmd : JsCmd =
  {
    JsRaw("var workspaceVar = " + pretty(JsonAST.render(workspaceJson)) + "; updateWorkspace(workspaceVar);").cmd
  }

  /**
   * JS Command which defines the createLinkingTask function
   */
  private def createLinkingTaskFunction : JsCmd =
  {
    def callback(projectName : String) : JsCmd =
    {
      CreateLinkingTaskDialog.projectName = Some(projectName)

      JsRaw("$('#createLinkingTaskDialog').dialog('open');").cmd
    }

    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName"), callback _)._2.cmd

    val initLinkingTaskDialog = OnLoad(JsRaw("$('#createLinkingTaskDialog').dialog({ autoOpen: false, width: 700, modal: true })").cmd)
    val openLinkingTaskDialog =  JsCmds.Function("createLinkingTask", "projectName" :: Nil, ajaxCall)

    initLinkingTaskDialog & openLinkingTaskDialog
  }

  /**
   * Opens a linking task from the workspace.
   */
  private def openLinkingTask(projectName : String, taskName : String)
  {
    User().workspace.projects.filter(_.name.toString == projectName).last.linkingModule.tasks.find(_.name == taskName) match
    {
      case Some(linkingTask) => User().linkingTask = linkingTask
      case None => throw new IllegalArgumentException("Linking Task '" + taskName + "' not found in project '" + projectName + "'.")
    }
  }

  /**
   * Removes a linking task from the workspace.
   */
  private def removeLinkingTask(projectName : String, taskName : String)
  {
    User().workspace.projects.filter(_.name == projectName).last.linkingModule.remove(taskName)
  }

  /*
   * Injects a Javascript function.
   */
  private def injectFunction(name : String, func : (String, String) => Unit, reload : Boolean = false) : JsCmd =
  {
    //Callback which executes the provided function
    def callback(args : String) : JsCmd =
    {
      try
      {
        val Array(projectName, taskName) = args.split(',')

        func(projectName, taskName)

        if(reload)
          updateWorkspaceCmd & JsRaw("window.location.reload();").cmd
        else
          updateWorkspaceCmd
      }
      catch
      {
        case ex : Exception => JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
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
  private def workspaceJson : JValue =
  {

    // TODO - Nested 'yield's seem to cause the (strange) compiler error: 'xxx is not an enclosing class'
    var projectList : List[JValue] = List()

    for(project <- User().workspace.projects.toSeq)
    {
      val sources : JArray = for(task <- project.sourceModule.tasks.toSeq) yield
      {
        ("name" -> task.name.toString) ~
        ("url" -> task.source.dataSource.toString)
      }

      val linkingTasks : JArray = for(task <- project.linkingModule.tasks.toSeq) yield
      {
        ("name" -> task.name.toString) ~
        ("source" -> task.linkSpec.datasets.source.sourceId.toString) ~
        ("target" -> task.linkSpec.datasets.target.sourceId.toString) ~
        ("sourceDataset" -> task.linkSpec.datasets.source.restriction) ~
        ("targetDataset" -> task.linkSpec.datasets.target.restriction)
      }

      val proj : JObject =
      {
        ("name" -> project.name.toString) ~
        ("dataSource" -> sources) ~
        ("linkingTask" -> linkingTasks)
      }

      projectList ::= proj
    }

    ("workspace" -> ("project" -> JArray(projectList)))
  }
}
