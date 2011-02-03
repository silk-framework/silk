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
import net.liftweb.util.Helpers._

/**
 * A dialog to create new linking tasks.
 */
class CreateLinkingTaskDialog
{
  def render(xhtml : NodeSeq) : NodeSeq =
  {
    var name = ""
    var sourceId = "http://www4.wiwiss.fu-berlin.de/sider/sparql"
    var targetId = "http://www4.wiwiss.fu-berlin.de/drugbank/sparql"
    var sourceRestriction = "?a rdf:type sider:drugs"
    var targetRestriction = "?b rdf:type drugbank:drugs"
    val prefixes = Map(
      "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
      "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
      "owl" -> "http://www.w3.org/2002/07/owl#",
      "sider" -> "http://www4.wiwiss.fu-berlin.de/sider/resource/sider/",
      "drugbank" -> "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/")

    def submit(prefixes : Prefixes) =
    {
      val linkSpec =
        LinkSpecification(
          id = "id",
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

    SHtml.ajaxForm(
      bind("entry", xhtml,
         "name" -> SHtml.text(name, name = _, "size" -> "60"),
         "sourceId" -> SHtml.text(sourceId, sourceId = _, "size" -> "60"),
         "sourceRestriction" -> SHtml.text(sourceRestriction, sourceRestriction = _, "size" -> "60"),
         "targetId" -> SHtml.text(targetId, targetId = _, "size" -> "60"),
         "targetRestriction" -> SHtml.text(targetRestriction, targetRestriction = _, "size" -> "60"),
         "prefixes" -> PrefixEditor.prefixEditor(prefixes),
         "submit" -> SHtml.ajaxSubmit("Create", () => PrefixEditor.readPrefixes(submit))))
  }
}