package de.fuberlin.wiwiss.silk.workbench.lift.util

import xml.NodeSeq
import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.OnLoad
import de.fuberlin.wiwiss.silk.workbench.lift.snippet.Workspace
import net.liftweb.http.js.JsCmd
import net.liftweb.common.Empty

/**
 * Basic dialog with a number of fields.
 */
trait Dialog
{
  /** The title of this dialog */
  val title : String

  /** The fields of this dialog. */
  val fields : List[StringField]

  /** The parameters of this dialog e.g. ("with" -> "700") */
  protected def dialogParams : List[(String, String)] = ("autoOpen" -> "false") :: ("width" -> "700") :: ("modal" -> "true") :: Nil

  /** The id of this dialog */
  private lazy val id : String = title.replace(" ", "")

  /**
   * Command which initializes this dialog.
   */
  def initCmd = OnLoad(JsRaw("$('#" + id + "').dialog({ " + dialogParams.map(_.productIterator.mkString(": ")).mkString(", ") + " })").cmd)

  /**
   * Command which opens this dialog.
   */
  def openCmd =
  {
    //Update all fields and open the dialog
    fields.map(_.updateValueCmd).reduceLeft(_ & _) &
    JsRaw("$('#" + id + "').dialog('open');").cmd
  }

  /**
   * Command which closes this dialog.
   */
  def closeCmd = JsRaw("$('#" + id + "').dialog('close');").cmd

  /**
   * Renders this dialog.
   */
  def render(xhtml : NodeSeq) : NodeSeq =
  {
    def submit() =
    {
      try
      {
        onSubmit()
        closeCmd & Workspace.updateCmd
      }
      catch
      {
        case ex : Exception => Workspace.hideLoadingDialogCmd & JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    <div id={id} title={title}>
    {
      SHtml.ajaxForm(
        <table>
        {
          for(field <- fields) yield
          {
            <tr>
              <td>
              { field.label }
              </td>
              <td>
              { field.render }
              </td>
            </tr>
          }
        }
          <tr>
            <td>
            </td>
            <td>
            { SHtml.ajaxSubmit("Save", submit) }
            </td>
          </tr>
        </table>
      )
    }
    </div>

  }

  /**
   * Called when the dialog is submitted.
   * Must be overloaded by sub classes in order to read the input values.
   */
  def onSubmit() : Unit

  /**
   * An input field.
   */
  sealed trait Field
  {
    val label : String

    val description : String

    var value : String

    protected lazy val id = Dialog.this.id + label.replace(" ", "")

    def render : NodeSeq

    def updateValueCmd : JsCmd
  }

  /**
   * A field which holds a string value
   */
  case class StringField(label : String, description : String, initialValue : () => String = (() => "")) extends Field
  {
    override var value = ""

    override def render = SHtml.text(value, value = _, "id" -> id, "size" -> "60", "title" -> description)

    override def updateValueCmd =
    {
      value = initialValue()
      JsRaw("$('#" + id + "').val('" + value + "');").cmd
    }
  }

  /**
   * A field which holds an enumeration of values which can be selected.
   */
  case class SelectField(label : String, description : String, allowedValues : () => Seq[String], initialValue : () => String) extends Field
  {
    override var value = ""

    override def render = SHtml.untrustedSelect(Nil, Empty, value = _, "id" -> id, "title" -> description)

    override def updateValueCmd =
    {
      value = initialValue()

      //Generate the options of the select box
      val options =
        for(allowedValue <- allowedValues()) yield
        {
          if(allowedValue == value)
          {
            <option value={allowedValue} selected="true">{allowedValue}</option>
          }
          else
          {
            <option value={allowedValue}>{allowedValue}</option>
          }
        }

      //Update select box
      JsRaw("$('#" + id + "').children().remove();").cmd &
      JsRaw("$('#" + id + "').append('" + options.mkString + "');").cmd
    }
  }
}