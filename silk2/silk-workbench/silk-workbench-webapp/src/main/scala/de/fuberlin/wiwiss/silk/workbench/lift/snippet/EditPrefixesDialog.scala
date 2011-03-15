package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.config.Prefixes
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmds.OnLoad
import net.liftweb.widgets.autocomplete.AutoComplete
import de.fuberlin.wiwiss.silk.workbench.lift.util.PrefixRegistry

/**
 * Dialog which allows the user to edit the current prefixes.
 */
object EditPrefixesDialog
{
  /** The id of the dialog */
  private val dialogId = "editPrefixesDialog"

  /** The id of the prefix table */
  private val tableId = "prefixesTable"

  private var rowCounter = 0

  /**
   * JavaScript command which initializes this dialog.
   */
  def initCmd = OnLoad(JsRaw("$('#" + dialogId + "').dialog({ autoOpen: false, width: 700, modal: true })").cmd)

  /**
   * JavaScript command which opens this dialog.
   */
  def openCmd =
  {
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

    bind("entry", xhtml,
         "prefixTable" -> <table id={tableId}><tr><td></td><td>{SHtml.ajaxButton("add", () => addRowCmd())}</td></tr></table>,
         "submit" -> SHtml.ajaxButton("Save", () => read(submit))
    )
  }

  private def addRowCmd(initialValue : String = "") =
  {
    //Generate a new row ID
    val rowId = "prefixRow" + rowCounter
    rowCounter += 1

    //Function which generates prefix suggestions
    def completePrefix(current : String, limit : Int) : Seq[String] =
    {
      val prefixes =
        for((id, namespace) <- PrefixRegistry.all
            if id.startsWith(current.takeWhile(_ != ':'))) yield
            id + ": " + namespace

      prefixes.toSeq.take(limit)
    }

    //Creates a new table row
    val row =
      <tr id={rowId}>
        <td>{AutoComplete(initialValue, completePrefix _, (v : String) => (), "size" -> "75")}</td>
        <td><button onclick={"$('#" + rowId + "').remove()"}>Remove</button></td>
      </tr>

    //Command which adds a new row
    JsRaw("$('#" + tableId + " tr').last().before(" + row.toString.encJs + ");").cmd
  }

  /**
   * Reads the prefix table.
   */
  private def read(f : Prefixes => JsCmd) : JsCmd =
  {
    def update(str : String) =
    {
      val prefixes = for(line <- str.split(',')) yield
      {
        val id = line.takeWhile(_ != ':').trim
        val namespace = line.dropWhile(_ != ':').drop(1).trim

        (id, namespace)
      }

      f(Prefixes(prefixes.toMap))
    }

    SHtml.ajaxCall(JsRaw("$('#" + tableId + " tr td input').toArray().map(function (a) { return a.value; })"), update)._2.cmd
  }
}