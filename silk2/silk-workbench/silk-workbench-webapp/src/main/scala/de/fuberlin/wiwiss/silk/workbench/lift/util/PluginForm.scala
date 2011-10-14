package de.fuberlin.wiwiss.silk.workbench.lift.util

import xml.NodeSeq
import net.liftweb.http.js.JsCmds._
import de.fuberlin.wiwiss.silk.util.plugin.{AnyPlugin, Parameter, PluginDescription}
import java.util.UUID

/**
 * A form which allows the user to create instances of a specific plugin.
 */
class PluginForm[T <: AnyPlugin](val plugin : PluginDescription[T], currentObj : () => Option[T]) {
  private val fields = plugin.parameters.map(createField)

  /** The id of this form */
  private lazy val id : String = UUID.randomUUID.toString

  def renderDescription(): NodeSeq = {
    <div id={"description-" + id} style="padding-top: 10px; padding-bottom: 10px;">
    { plugin.description }
    </div>
  }

  /**
   * Renders this form to HTML.
   */
  def render(): NodeSeq = {
    <div id={"plugin-" + id}> {
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
      </table>
    }
    </div>
  }

  /**
   * Updates this form.
   */
  def updateCmd(selectedPlugin : PluginDescription[T]) = {
    val cmd = fields.map(_.updateValueCmd).fold(JS.Empty)(_ & _)

    if(plugin.id == selectedPlugin.id)
      cmd & JsShowId("description-" + id) & JsShowId("plugin-" + id)
    else
      cmd & JsHideId("description-" + id) & JsHideId("plugin-" + id)
  }

  /**
   * Creates a new instance of the plugin based on the entered values.
   */
  def create() = {
    val paramValues = fields.map(field => (field.label, field.value)).toMap

    plugin(paramValues)
  }

  private def createField(param: Parameter) = {
    def value() = {
      currentObj() match {
        case Some(obj) if obj.pluginId == plugin.id => Option(param(obj)).getOrElse("").toString
        case _ => param.defaultValue.flatMap(Option(_)).getOrElse("").toString
      }
    }

    StringField(param.name, param.description, value)
  }
}