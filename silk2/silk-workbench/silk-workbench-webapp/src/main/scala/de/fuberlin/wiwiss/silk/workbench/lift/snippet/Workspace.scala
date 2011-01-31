package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml
import net.liftweb.json.JsonAST.{JArray, JValue}
import net.liftweb.http.js.JE.{JsRaw, JsVar}
import net.liftweb.json.JsonDSL._
import net.liftweb.http.js.JsCmds.{Script, JsReturn}
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.js.{JE, JsCmds}
import net.liftweb.json.JsonAST
import net.liftweb.http.js.JsCmds.{Script, OnLoad}
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.linkspec.{LinkFilter, LinkCondition, DatasetSpecification, LinkSpecification}
import de.fuberlin.wiwiss.silk.workbench.Constants
import de.fuberlin.wiwiss.silk.workbench.project.Cache
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.evaluation.Alignment

class Workspace
{
  def toolbar(xhtml : NodeSeq) : NodeSeq =
  {
    def showCreateDialog() =
    {
      JsRaw("$('#createProjectDialog').dialog('open')").cmd
    }

    val initCreateDialog = Script(OnLoad(JsRaw("$('#createProjectDialog').dialog({ autoOpen: false, width: 700, modal: true })").cmd))

    bind("entry", xhtml,
         "new" -> (initCreateDialog ++ SHtml.ajaxButton("New", showCreateDialog _)))
  }

  def createProjectDialog(xhtml : NodeSeq) : NodeSeq =
  {
    var name = "New Project"
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

    def submit(prefixes : Map[String, String]) =
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

      val linkingTask = LinkingTask(name, new Prefixes(prefixes), linkSpec, Alignment(), new Cache())

      User().project.linkingModule.update(linkingTask)

      JsRaw("$('#createProjectDialog').dialog('close'); document.forms['toolbarForm'].submit();").cmd
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

  def content(xhtml : NodeSeq) : NodeSeq =
  {
    bind("entry", xhtml,
         "workspaceVar" -> Script(JsRaw("var workspaceVar = " + pretty(JsonAST.render(generateJson)) + ";").cmd))
  }

  private def generateJson : JValue =
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
      ("name" -> "project") ~
      ("dataSource" -> sources) ~
      ("linkingTask" -> linkingTasks)
    }

    ("workspace" -> ("project" -> projects))
  }
}
