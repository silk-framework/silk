package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.linkspec.{LinkCondition, LinkFilter, DatasetSpecification, LinkSpecification}
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.Constants
import de.fuberlin.wiwiss.silk.evaluation.Alignment
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.{Cache, LinkingTask}
import net.liftweb.http.js.JsCmds.OnLoad
import net.liftweb.common.Empty

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
    val prefixes = Map(
      "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
      "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
      "owl" -> "http://www.w3.org/2002/07/owl#",
      "foaf" -> "http://xmlns.com/foaf/0.1/")

    def submit(prefixes : Prefixes) =
    {
      try
      {
        val sourceNames = User().project.sourceModule.tasks.map(_.name.toString).toList
        require(sourceNames.contains(sourceId), "Source '" + sourceId + "' does not exist")
        require(sourceNames.contains(targetId), "Target '" + targetId + "' does not exist")

        val linkSpec =
          LinkSpecification(
            id = name,
            linkType = "http://www.w3.org/2002/07/owl#sameAs",
            datasets = SourceTargetPair(DatasetSpecification(sourceId, Constants.SourceVariable, sourceRestriction),
                                        DatasetSpecification(targetId, Constants.TargetVariable, targetRestriction)),
            condition = LinkCondition(None),
            filter = LinkFilter(0.95, None),
            outputs = Nil
          )

        val linkingTask = LinkingTask(name, prefixes, linkSpec, Alignment(), new Cache())

        User().project.linkingModule.update(linkingTask)

        JsRaw("$('#createLinkingTaskDialog').dialog('close');").cmd & Workspace.updateWorkspaceCmd
      }
      catch
      {
        case ex : Exception => JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    SHtml.ajaxForm(
      bind("entry", xhtml,
         "name" -> SHtml.text(name, name = _, "size" -> "60"),
         "sourceId" -> SHtml.untrustedSelect(Nil, Empty, sourceId = _, "id" -> "selectSourceId"),
         "sourceRestriction" -> SHtml.text(sourceRestriction, sourceRestriction = _, "size" -> "60"),
         "targetId" -> SHtml.untrustedSelect(Nil, Empty, targetId = _, "id" -> "selectTargetId"),
         "targetRestriction" -> SHtml.text(targetRestriction, targetRestriction = _, "size" -> "60"),
         "prefixes" -> PrefixEditor.prefixEditor(prefixes),
         "submit" -> SHtml.ajaxSubmit("Create", () => PrefixEditor.readPrefixes(submit))))
  }
}

object CreateLinkingTaskDialog
{
  def initCmd = OnLoad(JsRaw("$('#createLinkingTaskDialog').dialog({ autoOpen: false, width: 700, modal: true })").cmd)

  def openCmd =
  {
    val sourceOptions = for(task <- User().project.sourceModule.tasks) yield <option value={task.name}>{task.name}</option>

    JsRaw("$('#selectSourceId').children().remove();").cmd &
    JsRaw("$('#selectSourceId').append('" + sourceOptions.toString + "');").cmd &
    JsRaw("$('#selectTargetId').children().remove();").cmd &
    JsRaw("$('#selectTargetId').append('" + sourceOptions.toString + "');").cmd &
    JsRaw("$('#createLinkingTaskDialog').dialog('open');").cmd
  }
}