package de.fuberlin.wiwiss.silk.workbench.lift.util

import de.fuberlin.wiwiss.silk.util.strategy.{Strategy, Parameter, StrategyDescription}
import de.fuberlin.wiwiss.silk.workbench.lift.snippet.Workspace
import net.liftweb.http.SHtml
import net.liftweb.http.js.JE.JsRaw
import java.util.UUID
import net.liftweb.util.Helpers._
import net.liftweb.common.Full
import net.liftweb.http.js.JsCmds._
import xml.NodeSeq

/**
 * A dialog which lets the user choose between different strategies.
 */
trait StrategyDialog[T <: Strategy] {
  /** The title of this dialog. */
  def title : String

  /** The fields of this form. */
  protected val fields = List[StringField]()

  /** The strategy which can be selected by the user. */
  protected val strategies : Seq[StrategyDescription[T]]

  /** The current strategy instance */
  protected def currentObj : Option[T]

  /** Executed when the form is submitted. */
  protected def onSubmit(instance : T)

  /** The parameters of this dialog e.g. ("with" -> "700") */
  protected def dialogParams : List[(String, String)] = ("autoOpen" -> "false") :: ("width" -> "700") :: ("modal" -> "true") :: Nil

  /** The id of this form */
  private lazy val id : String = UUID.randomUUID.toString

  /** All strategy forms */
  private lazy val strategyForms = strategies.map(new StrategyForm(_, () => currentObj))

  /** The current form */
  private def currentForm = currentObj match {
    case Some(obj) => strategyForms.find(_.strategy.id == obj.strategyId).getOrElse(strategyForms.head)
    case None => strategyForms.head
  }

  /**
   * Renders this dialog.
   */
  def render(in : NodeSeq) : NodeSeq = {
    var selectedForm = currentForm

    def submit() = {
      try {
        onSubmit(selectedForm.create())

        Commands.close & Workspace.updateCmd
      } catch {
        case ex : Exception => {
          Workspace.hideLoadingDialogCmd & Alert(ex.getMessage.encJs)
        }
      }
    }

    def updateForm(form : StrategyForm[T]) = {
      selectedForm = form
      strategyForms.map(_.updateCmd(selectedForm.strategy)).reduce(_ & _)
    }

    <div id={id} title={title}>
      <div id={id + "-select"}>
      { SHtml.ajaxSelectObj(strategyForms.map(f => (f, f.strategy.label)), Full(selectedForm), updateForm) }
      </div> {
        SHtml.ajaxForm(
          <table> {
            for(field <- fields) yield {
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
          </table> ++
          strategyForms.map(_.render()).reduce(_ ++ _) ++
          SHtml.ajaxSubmit("Save", submit))
      }
    </div>
  }

  /**
   * JavaScript Commands.
   */
  object Commands {
    /**
     * Command which initializes this dialog.
     */
    def init = OnLoad(JsRaw("$('#" + id + "').dialog({ " + dialogParams.map(_.productIterator.mkString(": ")).mkString(", ") + " })").cmd)

    /**
     * Command which opens this dialog.
     */
    def open = {
      //Update all fields and open the dialog
      val updateFields = fields.map(_.updateValueCmd).reduceLeft(_ & _)
      val resetSelect = JsRaw("$('#" + id + "-select select option').removeAttr('selected')")
      val updateSelect = JsRaw("$('#" + id + "-select select option:contains(\"" + currentForm.strategy.label + "\")').attr('selected', 'selected')")
      val updateForms = strategyForms.map(_.updateCmd(currentForm.strategy)).reduce(_ & _)
      val openDialog = JsRaw("$('#" + id + "').dialog('open');").cmd

      updateFields & resetSelect & updateSelect & updateForms & openDialog
    }

    /**
     * Command which closes this dialog.
     */
    def close = JsRaw("$('#" + id + "').dialog('close');").cmd
  }
}