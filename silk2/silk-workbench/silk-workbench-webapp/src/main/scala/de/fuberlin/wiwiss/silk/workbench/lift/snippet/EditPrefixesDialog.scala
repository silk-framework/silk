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
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmds.OnLoad
import net.liftweb.widgets.autocomplete.AutoComplete
import de.fuberlin.wiwiss.silk.workbench.util.PrefixRegistry
import de.fuberlin.wiwiss.silk.workbench.util.PrefixRegistry
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd

/**
 * Dialog which allows the user to edit the current prefixes.
 */
object EditPrefixesDialog {
  /**The id of the dialog */
  private val dialogId = "editPrefixesDialog"

  /**The id of the prefix table */
  private val tableId = "prefixesTable"

  /**Counts the added rows in order to generate unique ids for them */
  private var rowCounter = 0

  /**
   * JavaScript command which initializes this dialog.
   */
  def initCmd = OnLoad(JsRaw("$('#" + dialogId + "').dialog({ autoOpen: false, width: 700, modal: true })").cmd)

  /**
   * JavaScript command which opens this dialog.
   */
  def openCmd = {
    //Update prefixes
    val prefixes = User().project.config.prefixes
    val removePrefixesCmd = JsRaw("$('#" + tableId + " tr').not(':last').remove();").cmd
    val addPrefixesCmd = prefixes.toSeq.sortBy(_._1)
                                 .map{ case (id, namespace) => addRowCmd(id + ": " + namespace) }
                                 .reduceLeft(_ & _)

    //Open dialog
    removePrefixesCmd & addPrefixesCmd & JsRaw("$('#" + dialogId + "').dialog('open');").cmd
  }

  /**
   * JavaScript command which closes this dialog.
   */
  def closeCmd = JsRaw("$('#" + dialogId + "').dialog('close');").cmd

  /**
   * Renders this dialog.
   */
  def render(xhtml: NodeSeq): NodeSeq = {
      bind("entry", xhtml,
           "prefixTable" -> <table id={tableId}><tr><td></td><td>{SHtml.ajaxButton("add", () => addRowCmd())}</td></tr></table>,
           "submit" -> SHtml.ajaxButton("Save", updatePrefixesCmd _)
      )
  }

  /**
   * Command which adds a new row to the prefix table.
   */
  private def addRowCmd(initialValue: String = "") = {
    //Generate a new row ID
    val rowId = "prefixRow" + rowCounter
    rowCounter += 1

    //Function which generates prefix suggestions
    def completePrefix(current: String, limit: Int): Seq[String] = {
      val prefixes =
        for((id, namespace) <- PrefixRegistry.all
            if id.startsWith(current.takeWhile(_ != ':'))) yield
            id + ": " + namespace

      prefixes.toSeq.take(limit)
    }

    //Creates a new table row
    val row =
      <tr id={rowId}>
        <td>{AutoComplete(initialValue, completePrefix _, (v : String) => (), "size" -> "75", "title" -> "Prefix id : namespace")}</td>
        <td><button onclick={"$('#" + rowId + "').remove()"}>Remove</button></td>
      </tr>

    //Command which adds a new row
    JsRaw("$('#" + tableId + " tr').last().before(" + row.toString.encJs + ");").cmd
  }

  /**
   * Command which reads the prefix table and updates the prefixes of the current project accordingly.
   */
  private def updatePrefixesCmd(): JsCmd = {
    def update(str: String) = {
      try {
        val prefixList = for (line <- str.split(',')) yield {
          //Parse line
          val id = line.takeWhile(_ != ':').trim
          val namespace = line.dropWhile(_ != ':').drop(1).trim

          //Validate
          if (id.isEmpty) throw new IllegalArgumentException("Prefix must not be empty")
          if (namespace.isEmpty) throw new IllegalArgumentException("Prefix muss have a valid namespace")

          (id, namespace)
        }

        User().project.config = User().project.config.copy(prefixes = prefixList.toMap)

        EditPrefixesDialog.closeCmd & Workspace.updateCmd
      } catch {
        case ex: Exception => Workspace.hideLoadingDialogCmd & JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    SHtml.ajaxCall(JsRaw("$(\"#" + tableId + " tr td input[type='text']\").toArray().map(function (a) { return a.value; })"), update)._2.cmd
  }
}