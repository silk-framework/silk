package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.http.js.JE.JsRaw
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import xml.NodeSeq
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.workbench.Constants
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.Cache
import net.liftweb.http.SHtml
import net.liftweb.common.Empty
import de.fuberlin.wiwiss.silk.linkspec._
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmds.{SetHtml, OnLoad}

/**
 * A dialog to edit linking tasks.
 */
class EditLinkingTaskDialog
{
  def render(xhtml : NodeSeq) : NodeSeq =
  {
    var sourceId = ""
    var targetId = ""
    var sourceRestriction = ""
    var targetRestriction = ""
    var linkType = ""
    val prefixes : Map[String, String] = Map.empty

    def submit() =
    {
      try
      {
        val linkingTask = User().linkingTask
        implicit val prefixes = User().project.config.prefixes

        val updatedDatasets = SourceTargetPair(DatasetSpecification(sourceId, Constants.SourceVariable, Restrictions.fromSparql(sourceRestriction)),
                                               DatasetSpecification(targetId, Constants.TargetVariable, Restrictions.fromSparql(targetRestriction)))

        val updatedLinkSpec = linkingTask.linkSpec.copy(datasets = updatedDatasets, linkType = linkType)

        val updatedLinkingTask = linkingTask.copy(linkSpec = updatedLinkSpec, cache = new Cache())

        User().project.linkingModule.update(updatedLinkingTask)
        User().closeTask()

        EditLinkingTaskDialog.closeCmd & Workspace.updateCmd
      }
      catch
      {
        case ex : Exception => Workspace.hideLoadingDialogCmd & JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    SHtml.ajaxForm(
      bind("entry", xhtml,
         "sourceId" -> SHtml.untrustedSelect(Nil, Empty, sourceId = _, "id" -> "editSourceId", "title" -> "Source dataset"),
         "sourceRestriction" -> SHtml.text(sourceRestriction, sourceRestriction = _, "id" -> "editSourceRes", "size" -> "60", "title" -> "Restrict source dataset using SPARQL clauses" ),
         "targetId" -> SHtml.untrustedSelect(Nil, Empty, targetId = _, "id" -> "editTargetId",  "title" -> "Target dataset"),
         "targetRestriction" -> SHtml.text(targetRestriction, targetRestriction = _, "id" -> "editTargetRes", "size" -> "60", "title" -> "Restrict target dataset using SPARQL clauses"),
         "linkType" -> SHtml.text(linkType, linkType = _, "id" -> "editLinkType", "size" -> "60",  "title" -> "Type of the generated link"),
         "submit" -> SHtml.ajaxSubmit("Save", submit)))
  }
}

object EditLinkingTaskDialog
{
  def initCmd = OnLoad(JsRaw("$('#editLinkingTaskDialog').dialog({ autoOpen: false, width: 700, modal: true, close : closeTask })").cmd)

  def openCmd =
  {
    val linkingTask = User().linkingTask
    val datasets = linkingTask.linkSpec.datasets
    val sourceTasks = User().project.sourceModule.tasks

    //Generate the options of the source select box
    val sourceOptions =
      for(task <- User().project.sourceModule.tasks) yield
      {
        if(task.name == datasets.source.sourceId)
        {
          <option value={task.name} selected="true">{task.name}</option>
        }
        else
        {
          <option value={task.name}>{task.name}</option>
        }
      }

    //Generate the options of the target select box
    val targetOptions =
      for(task <- User().project.sourceModule.tasks) yield
      {
        if(task.name == datasets.target.sourceId)
        {
          <option value={task.name} selected="true">{task.name}</option>
        }
        else
        {
          <option value={task.name}>{task.name}</option>
        }
      }

    //Update source lists
    JsRaw("$('#editSourceId').children().remove();").cmd &
    JsRaw("$('#editSourceId').append('" + sourceOptions.mkString + "');").cmd &
    JsRaw("$('#editTargetId').children().remove();").cmd &
    JsRaw("$('#editTargetId').append('" + targetOptions.mkString + "');").cmd &
    //Update restrictions
    JsRaw("$('#editSourceRes').val('" + datasets.source.restriction + "');").cmd &
    JsRaw("$('#editTargetRes').val('" + datasets.target.restriction + "');").cmd &
    //Update link type
    JsRaw("$('#editLinkType').val('" + linkingTask.linkSpec.linkType + "');").cmd &
    //Open dialog
    JsRaw("$('#editLinkingTaskDialog').dialog('open');").cmd
  }

  def closeCmd = JsRaw("$('#editLinkingTaskDialog').dialog('close');").cmd
}