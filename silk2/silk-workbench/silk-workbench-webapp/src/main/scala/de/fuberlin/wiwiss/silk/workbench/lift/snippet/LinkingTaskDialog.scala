/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.Constants
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinks
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.{Cache, LinkingTask}
import de.fuberlin.wiwiss.silk.entity.SparqlRestriction
import de.fuberlin.wiwiss.silk.config.{LinkFilter, DatasetSpecification, LinkSpecification}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS
import net.liftweb.http.js.JsCmds.{SetHtml, OnLoad}
import de.fuberlin.wiwiss.silk.util.{Identifier, DPair}
import net.liftweb.common.Full

/**
 * A dialog to create and edit linking tasks.
 */
class LinkingTaskDialog {

  /** Render the dialog */
  def render(xhtml: NodeSeq): NodeSeq = {
    <div id={LinkingTaskDialog.id} title="Linking Task">
    </div>
  }
}

object LinkingTaskDialog {
  private val id = "linkingTaskDialog"

  def initCmd = OnLoad(JsRaw("$('#" + id + "').dialog({ autoOpen: false, width: 700, modal: true })").cmd)

  /** Updates and opens the dialog */
  def openCmd = SetHtml(id, renderDialog()) & JsRaw("$('#" + id + "').dialog('open');").cmd

  def closeCmd = JsRaw("$('#" + id + "').dialog('close');").cmd

  private def renderDialog(): NodeSeq = {
    require(User().projectOpen, "LinkingTaskDialog requires an open project")

    val task = if(User().linkingTaskOpen) Some(User().linkingTask) else None

    //Generate the list of all available source ids
    val sourceIds = for(task <- User().project.sourceModule.tasks.toSeq) yield (task.name.toString, task.name.toString)

    //Variables to hold the values of the current linking task
    var name = task.map(_.name.toString).getOrElse("")
    var sourceId = task.map(_.linkSpec.datasets.source.sourceId.toString).getOrElse(sourceIds.head._1)
    var targetId = task.map(_.linkSpec.datasets.target.sourceId.toString).getOrElse(sourceIds.head._1)
    var sourceRestriction = task.map(_.linkSpec.datasets.source.restriction.toSparql).getOrElse("?a rdf:type myprefix:myclass")
    var targetRestriction = task.map(_.linkSpec.datasets.target.restriction.toSparql).getOrElse("?b rdf:type myprefix:myclass")
    var linkType = task.map(_.linkSpec.linkType.toString).getOrElse("http://www.w3.org/2002/07/owl#sameAs")

    /** Commits the linking task when the dialog is submitted. */
    def submit() = JS.Try(){
      implicit val prefixes = User().project.config.prefixes

      val datasets = DPair(DatasetSpecification(sourceId, Constants.SourceVariable, SparqlRestriction.fromSparql(sourceRestriction)),
                           DatasetSpecification(targetId, Constants.TargetVariable, SparqlRestriction.fromSparql(targetRestriction)))

      if(User().linkingTaskOpen) {
        val linkingTask = User().linkingTask
        val updatedLinkSpec = linkingTask.linkSpec.copy(id = name, datasets = datasets, linkType = linkType)
        val updatedLinkingTask = linkingTask.updateLinkSpec(updatedLinkSpec, User().project)

        if(linkingTask.name != updatedLinkingTask.name) {
          User().project.linkingModule.remove(linkingTask.name)
        }
        User().project.linkingModule.update(updatedLinkingTask)
        User().closeTask()
      } else {
        val linkSpec =
          LinkSpecification(
            id = name,
            linkType = linkType,
            datasets = datasets,
            rule = LinkageRule(None),
            filter = LinkFilter(),
            outputs = Nil
          )

        val linkingTask = LinkingTask(User().project, linkSpec, ReferenceLinks())
        User().project.linkingModule.update(linkingTask)
      }

      LinkingTaskDialog.closeCmd & Workspace.updateCmd
    }

    def updateSourceId(id: String) = {
      sourceId = id
      JS.Empty
    }

    def updateTargetId(id: String) = {
      targetId = id
      JS.Empty
    }

    //Render the dialog
    SHtml.ajaxForm(
      <table border="0">
        <tr>
          <td>Name</td>
          <td>{ SHtml.text(name, name = _, "size" -> "60", "title" -> "Linking task name") }</td>
        </tr>
        <tr>
          <td>Source</td>
          <td>{ SHtml.ajaxSelect(sourceIds, Full(sourceId), updateSourceId _, "title" -> "Source dataset") }</td>
        </tr>
        <tr>
          <td>Source restriction</td>
          <td>{ SHtml.text(sourceRestriction, sourceRestriction = _, "size" -> "60", "title" -> "Restrict source dataset using SPARQL clauses") }</td>
        </tr>
        <tr>
          <td>Target</td>
          <td>{ SHtml.ajaxSelect(sourceIds, Full(targetId), updateTargetId _, "title" -> "Target dataset") }</td>
        </tr>
        <tr>
          <td>Target restriction</td>
          <td>{ SHtml.text(targetRestriction, targetRestriction = _, "size" -> "60", "title" -> "Restrict target dataset using SPARQL clauses") }</td>
        </tr>
        <tr>
          <td>Link type</td>
          <td>{ SHtml.text(linkType, linkType = _, "size" -> "60", "title" -> "Type of the generated link") }</td>
        </tr>
        <tr>
          <td></td>
          <td align="right">{ SHtml.ajaxSubmit("Apply", submit) }</td>
        </tr>
      </table>
    )
  }
}