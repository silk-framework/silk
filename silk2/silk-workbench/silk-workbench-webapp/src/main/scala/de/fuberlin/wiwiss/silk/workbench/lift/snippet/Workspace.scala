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
    val updateWorkspace = Script(JsRaw("var workspaceVar = " + pretty(JsonAST.render(generateWorkspaceJson)) + "; updateWorkspace(workspaceVar);").cmd)

    bind("entry", xhtml,
         "injectedJavascript" -> (updateWorkspace ++ injectFunction("removeLinkingTask", removeLinkingTask _)))
  }

  private def generateWorkspaceJson : JValue =
  {
    val project = User().project

    val sources : JArray = for(task <- project.sourceModule.tasks.toSeq) yield
    {
      ("name" -> task.name) ~
      ("url" -> task.source.dataSource.toString)
    }

    val linkingTasks : JArray = for(task <- project.linkingModule.tasks.toSeq) yield
    {
      ("name" -> task.name) ~
      ("source" -> task.linkSpec.datasets.source.sourceId) ~
      ("target" -> task.linkSpec.datasets.target.sourceId) ~
      ("sourceDataset" -> task.linkSpec.datasets.source.restriction) ~
      ("targetDataset" -> task.linkSpec.datasets.target.restriction)
    }

    val projects : JArray = for(p <- project :: Nil) yield
    {
      ("name" -> project.name) ~
      ("dataSource" -> sources) ~
      ("linkingTask" -> linkingTasks)
    }

    ("workspace" -> ("project" -> projects))
  }

  private def removeLinkingTask(projectName : String, taskName : String)
  {
    //Ignoring the projectName for now until we have an complete workspace
    User().project.linkingModule.remove(taskName)
  }

  //Injects a Javascript function into HTML
  //TODO generalize and move to JavaScriptUtils?
  private def injectFunction(name : String, func : (String, String) => Unit) : Node =
  {
    //Callback which executes the provided function
    def callback(args : String) : JsCmd =
    {
      try
      {
        val Array(projectName, taskName) = args.split(',')

        func(projectName, taskName)

        //Update the workspace
        JsRaw("var workspaceVar = " + pretty(JsonAST.render(generateWorkspaceJson)) + "; updateTreeview(workspaceVar").cmd
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
    val functionDef = JsCmds.Function(name, "projectName" :: "taskName" :: Nil, ajaxCall)

    Script(functionDef)
  }
}
