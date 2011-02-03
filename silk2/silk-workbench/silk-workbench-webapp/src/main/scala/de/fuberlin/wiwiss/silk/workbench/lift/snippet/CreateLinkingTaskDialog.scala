package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.linkspec.{LinkCondition, LinkFilter, DatasetSpecification, LinkSpecification}
import de.fuberlin.wiwiss.silk.workbench.project.Cache
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.Constants
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.evaluation.Alignment
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.SHtml
import net.liftweb.widgets.autocomplete.AutoComplete
import net.liftweb.util.Helpers._

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
    var sourceRestriction = "?a rdf:type sider:drugs"
    var targetRestriction = "?b rdf:type drugbank:drugs"
    val prefixes = Map(
      "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
      "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
      "owl" -> "http://www.w3.org/2002/07/owl#",
      "sider" -> "http://www4.wiwiss.fu-berlin.de/sider/resource/sider/",
      "drugbank" -> "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/")

    val sourceNames = User().project.sourceModule.tasks.map(_.name.toString).toList

    def completeSourceId(current : String, limit: Int) =
    {
      sourceNames.filter(_.startsWith(current)).take(limit)
    }

    def submit(prefixes : Prefixes) =
    {
      try
      {
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

        JsRaw("$('#createLinkingTaskDialog').dialog('close'); document.forms['toolbarForm'].submit();").cmd
      }
      catch
      {
        case ex : Exception => JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    SHtml.ajaxForm(
      bind("entry", xhtml,
         "name" -> SHtml.text(name, name = _, "size" -> "60"),
         "sourceId" -> AutoComplete(sourceId, completeSourceId _, (id : String) => sourceId = id, "size" -> "60"),
         "sourceRestriction" -> SHtml.text(sourceRestriction, sourceRestriction = _, "size" -> "60"),
         "targetId" -> AutoComplete(targetId, completeSourceId _, (id : String) => targetId = id, "size" -> "60"),
         "targetRestriction" -> SHtml.text(targetRestriction, targetRestriction = _, "size" -> "60"),
         "prefixes" -> PrefixEditor.prefixEditor(prefixes),
         "submit" -> SHtml.ajaxSubmit("Create", () => PrefixEditor.readPrefixes(submit))))
  }
}