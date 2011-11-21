/* 
 * Copyright 2011 Freie Universität Berlin, MediaEvent Services GmbH & Co. KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workbench.lift.util

import de.fuberlin.wiwiss.silk.util.plugin.{AnyPlugin, Parameter, PluginDescription}
import de.fuberlin.wiwiss.silk.workbench.lift.snippet.Workspace
import net.liftweb.http.SHtml
import net.liftweb.http.js.JE.JsRaw
import java.util.UUID
import net.liftweb.util.Helpers._
import net.liftweb.common.Full
import net.liftweb.http.js.JsCmds._
import xml.NodeSeq

/**
 * A dialog which lets the user choose between different plugins.
 */
trait PluginDialog[T <: AnyPlugin] {
  /** The title of this dialog. */
  def title : String

  /** The fields of this form. */
  protected val fields = List[StringField]()

  /** The plugins which can be selected by the user. */
  protected val plugins : Seq[PluginDescription[T]]

  /** The current plugin instance */
  protected def currentObj : Option[T]

  /** Executed when the form is submitted. */
  protected def onSubmit(instance : T)

  /** The parameters of this dialog e.g. ("with" -> "700") */
  protected def dialogParams : List[(String, String)] = ("autoOpen" -> "false") :: ("width" -> "700") :: ("modal" -> "true") :: Nil

  /** The id of this form */
  private lazy val id : String = UUID.randomUUID.toString

  /** All plugin forms */
  private lazy val pluginForms = plugins.map(new PluginForm(_, () => currentObj))

  /** The current form */
  private def currentForm = currentObj match {
    case Some(obj) => pluginForms.find(_.plugin.id == obj.pluginId).getOrElse(pluginForms.head)
    case None => pluginForms.head
  }

  /**
   * The selected plugin form
   */
  @volatile private var selectedForm: PluginForm[T] = _

  /**
   * Renders this dialog.
   */
  def render(in : NodeSeq) : NodeSeq = {
    selectedForm = currentForm

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

    def updateForm(form : PluginForm[T]) = {
      selectedForm = form
      pluginForms.map(_.updateCmd(selectedForm.plugin)).reduce(_ & _)
    }

    <div id={id} title={title}>
      <div id={id + "-select"}>
      { SHtml.ajaxSelectObj(pluginForms.map(f => (f, f.plugin.label)), Full(selectedForm), updateForm) }
      </div> {
        pluginForms.map(_.renderDescription()).reduce(_ ++ _) ++
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
          pluginForms.map(_.render()).reduce(_ ++ _) ++
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
      //Update the selected form
      selectedForm = currentForm

      //Update all fields and open the dialog
      val updateFields = fields.map(_.updateValueCmd).reduceLeft(_ & _)
      val resetSelect = JsRaw("$('#" + id + "-select select option').removeAttr('selected')")
      val updateSelect = JsRaw("$('#" + id + "-select select option:contains(\"" + currentForm.plugin.label + "\")').attr('selected', 'selected')")
      val updateForms = pluginForms.map(_.updateCmd(currentForm.plugin)).reduce(_ & _)
      val openDialog = JsRaw("$('#" + id + "').dialog('open');").cmd

      updateFields & resetSelect & updateSelect & updateForms & openDialog
    }

    /**
     * Command which closes this dialog.
     */
    def close = JsRaw("$('#" + id + "').dialog('close');").cmd
  }
}