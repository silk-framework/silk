package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.config.Prefixes
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmds.{SetHtml, OnLoad}

/**
 * Dialog which allows the user to edit the current prefixes.
 */
object EditPrefixesDialog
{
  /** The id of the dialog */
  private val dialogId = "editPrefixesDialog"

  /** The id of the prefix table */
  private val tableId = "prefixesTable"

  /**
   * JavaScript command which initializes this dialog.
   */
  def initCmd = OnLoad(JsRaw("$('#" + dialogId + "').dialog({ autoOpen: false, width: 700, modal: true })").cmd)

  /**
   * JavaScript command which opens this dialog.
   */
  def openCmd =
  {
    val prefixes = User().project.config.prefixes

    //Update prefixes
    SetHtml("editPrefixes", createTable(prefixes)) &
    //Open dialog
    JsRaw("$('#" + dialogId + "').dialog('open');").cmd
  }

  /**
   * JavaScript command which closes this dialog.
   */
  def closeCmd = JsRaw("$('#" + dialogId + "').dialog('close');").cmd

  /**
   * Renders this dialog.
   */
  def render(xhtml : NodeSeq) : NodeSeq =
  {
    def submit(prefixes : Prefixes) =
    {
      try
      {
        User().project.config = User().project.config.copy(prefixes = prefixes)

        EditPrefixesDialog.closeCmd & Workspace.updateCmd
      }
      catch
      {
        case ex : Exception => Workspace.hideLoadingDialogCmd & JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    SHtml.ajaxForm(
      bind("entry", xhtml,
           "prefixTable" -> <div id="editPrefixes" />,
           "submit" -> SHtml.ajaxSubmit("Save", () => read(submit)))
    )
  }

  /**
   * Creates the prefix table.
   */
  private def createTable(prefixes : Map[String, String] = Map.empty) : NodeSeq =
  {
    def addRow() =
    {
      JsRaw("$('#" + tableId + "').append(\"<tr><td><input type='text' title='Prefix id' /></td><td><input type='text' size='50' title='Prefix namespace'/></td></tr>\");").cmd
    }

    def removeRow() =
    {
      JsRaw("$('#" + tableId + " tr td').parent().last().remove();").cmd
    }

    <p>
    <table id={tableId}>
      <tr>
        <th>Prefix</th>
        <th>Namespace</th>
      </tr>
      {
        for((prefix, namespace) <- prefixes) yield
        {
          <tr>
            <td><input type='text' value={prefix} title='Prefix id'/></td>
            <td><input type='text' value={namespace} size="50" title='Prefix namespace' /></td>
          </tr>
        }
      }
    </table>
    {SHtml.ajaxButton("add", addRow _)}
    {SHtml.ajaxButton("remove", removeRow _)}
    </p>
  }

  /**
   * Reads the prefix table.
   */
  private def read(f : Prefixes => JsCmd) : JsCmd =
  {
    def update(str : String) =
    {
      val prefixes = for(Array(prefix, namespace) <- str.split(',').grouped(2)) yield (prefix, namespace)

      f(Prefixes(prefixes.toMap))
    }

    SHtml.ajaxCall(JsRaw("$('#" + tableId + " tr td input').toArray().map(function (a) { return a.value; })"), update)._2.cmd
  }
}