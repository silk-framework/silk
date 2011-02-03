package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml
import net.liftweb.json.JsonAST.{JArray, JValue}
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.json.JsonDSL._
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.json.JsonAST
import net.liftweb.http.js.JsCmds.{Script, OnLoad}
import xml.{Node, NodeSeq}
import net.liftweb.http.js.{JsCmd, JsCmds}

/**
 * Workspace snippet.
 *
 * Injects the following functions:
 *
 * def removeLinkingTask(
 *     projectName : The name of the project,
 *     taskName : The name of the task to be removed,
 * )
 *
 * def loadLinkingTask(
 *     projectName : The name of the project,
 *     taskName : The name of the task to be removed,
 * )
 *
 * def updateLinkingTask(
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
//TODO implement loadLinkingTask and updateLinkingTask
class Workspace
{
  def toolbar(xhtml : NodeSeq) : NodeSeq =
  {
    def showCreateDialog() =
    {
      JsRaw("$('#createLinkingTaskDialog').dialog('open')").cmd
    }

    val initCreateDialog = Script(OnLoad(JsRaw("$('#createLinkingTaskDialog').dialog({ autoOpen: false, width: 700, modal: true })").cmd))

    bind("entry", xhtml,
         "new" -> (initCreateDialog ++ SHtml.ajaxButton("New", showCreateDialog _)))
  }

  def content(xhtml : NodeSeq) : NodeSeq =
  {
    bind("entry", xhtml,
         "injectedJavascript" -> (Script(updateWorkspaceCmd & injectFunction("removeLinkingTask", removeLinkingTask _))))
  }

  /**
   * Removes a linking task from the workspace.
   */
  private def removeLinkingTask(projectName : String, taskName : String)
  {
    //Ignoring the projectName for now until we have an complete workspace
    User().project.linkingModule.remove(taskName)
  }

  /**
   * JS Command which updates the workspace view.
   */
  private def updateWorkspaceCmd : JsCmd =
  {
    JsRaw("var workspaceVar = " + pretty(JsonAST.render(workspaceJson)) + "; updateWorkspace(workspaceVar);").cmd
  }

  /**
   * Generates a JSON which contains the workspace contents.
   */
  private def workspaceJson : JValue =
  {
    val project = User().project

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

    val projects : JArray = for(p <- project :: Nil) yield
    {
      ("name" -> project.name.toString) ~
      ("dataSource" -> sources) ~
      ("linkingTask" -> linkingTasks)
    }

    ("workspace" -> ("project" -> projects))
  }

  /*
   * Injects a Javascript function.
   */
  private def injectFunction(name : String, func : (String, String) => Unit) : JsCmd =
  {
    //Callback which executes the provided function
    def callback(args : String) : JsCmd =
    {
      try
      {
        val Array(projectName, taskName) = args.split(',')

        func(projectName, taskName)

        updateWorkspaceCmd
      }
      catch
      {
        case ex : Exception => JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    //Ajax Call which executes the callback
    //TODO serialize arguments correctly not using a comma as separator which might also be used in the argument value itself
    val ajaxCall = SHtml.ajaxCall(JsRaw("projectName + ',' + taskName"), callback _)._2.cmd

    //JavaScript function definition
    JsCmds.Function(name, "projectName" :: "taskName" :: Nil, ajaxCall)
  }
}
