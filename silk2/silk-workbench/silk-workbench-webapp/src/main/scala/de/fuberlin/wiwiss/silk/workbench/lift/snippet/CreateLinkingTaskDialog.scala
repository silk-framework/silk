package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.linkspec.condition.{LinkCondition}
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.Constants
import de.fuberlin.wiwiss.silk.evaluation.Alignment
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.{Cache, LinkingTask}
import net.liftweb.http.js.JsCmds.OnLoad
import net.liftweb.common.Empty
import de.fuberlin.wiwiss.silk.linkspec.{Restrictions, LinkFilter, DatasetSpecification, LinkSpecification}

/**
 * A dialog to create new linking tasks.
 */
class CreateLinkingTaskDialog
{
  def render(xhtml : NodeSeq) : NodeSeq =
  {
    var name = ""
    var sourceId = ""
    var targetId = ""
    var sourceRestriction = ""
    var targetRestriction = ""
    var linkType = "http://www.w3.org/2002/07/owl#sameAs"

    def submit() =
    {
      try
      {
        implicit val prefixes = User().project.config.prefixes

        val linkSpec =
          LinkSpecification(
            id = name,
            linkType = linkType,
            datasets = SourceTargetPair(DatasetSpecification(sourceId, Constants.SourceVariable, Restrictions.fromSparql(sourceRestriction)),
                                        DatasetSpecification(targetId, Constants.TargetVariable, Restrictions.fromSparql(targetRestriction))),
            condition = LinkCondition(None),
            filter = LinkFilter(0.95, None),
            outputs = Nil
          )

        val linkingTask = LinkingTask(name, linkSpec, Alignment(), new Cache())

        User().project.linkingModule.update(linkingTask)

        CreateLinkingTaskDialog.closeCmd & Workspace.updateCmd
      }
      catch
      {
        case ex : Exception => Workspace.hideLoadingDialogCmd & JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    SHtml.ajaxForm(
      bind("entry", xhtml,
         "name" -> SHtml.text(name, name = _, "size" -> "60",  "title" -> "Linking task name"),
         "sourceId" -> SHtml.untrustedSelect(Nil, Empty, sourceId = _, "id" -> "selectSourceId", "title" -> "Source dataset"),
         "sourceRestriction" -> SHtml.text(sourceRestriction, sourceRestriction = _, "size" -> "60", "title" -> "Restrict source dataset using SPARQL clauses" ),
         "targetId" -> SHtml.untrustedSelect(Nil, Empty, targetId = _, "id" -> "selectTargetId",  "title" -> "Target dataset"),
         "targetRestriction" -> SHtml.text(targetRestriction, targetRestriction = _, "size" -> "60", "title" -> "Restrict target dataset using SPARQL clauses"),
         "linkType" -> SHtml.text(linkType, linkType = _, "size" -> "60",  "title" -> "Type of the generated link"),
         "submit" -> SHtml.ajaxSubmit("Create", submit)))
  }
}

object CreateLinkingTaskDialog
{
  def initCmd = OnLoad(JsRaw("$('#createLinkingTaskDialog').dialog({ autoOpen: false, width: 700, modal: true })").cmd)

  def openCmd =
  {
    val sourceOptions = for(task <- User().project.sourceModule.tasks) yield <option value={task.name}>{task.name}</option>

    JsRaw("$('#selectSourceId').children().remove();").cmd &
    JsRaw("$('#selectSourceId').append('" + sourceOptions.mkString + "');").cmd &
    JsRaw("$('#selectTargetId').children().remove();").cmd &
    JsRaw("$('#selectTargetId').append('" + sourceOptions.mkString + "');").cmd &
    JsRaw("$('#createLinkingTaskDialog').dialog('open');").cmd
  }

  def closeCmd = JsRaw("$('#createLinkingTaskDialog').dialog('close');").cmd
}