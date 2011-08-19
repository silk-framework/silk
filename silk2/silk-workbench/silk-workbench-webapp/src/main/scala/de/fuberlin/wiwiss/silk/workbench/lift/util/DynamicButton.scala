package de.fuberlin.wiwiss.silk.workbench.lift.util

import net.liftweb.http.{SHtml, CometActor}
import xml.Text
import java.util.UUID
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd

/**
 * A button whose properties can be changed at runtime.
 */
trait DynamicButton extends CometActor  {

  /** The id of the HTML button. */
  private val id = UUID.randomUUID.toString

  /** True, if this button already has been rendered. */
  @volatile private var rendered = false

  /** The current label of this button. */
  @volatile private var labelVar = "Button"

  /** True if this button is currently enabled. */
  @volatile private var enabledVar = true

  /**
   * Called if the button has been pressed.
   */
  protected def onPressed(): JsCmd

  /**
   * Renders this button.
   */
  override def render = {
    rendered = true
    if(enabled)
      SHtml.ajaxButton(Text(label), onPressed _, ("id" -> id))
    else
      SHtml.ajaxButton(Text(label), onPressed _, ("id" -> id), ("disabled" -> "disabled"))
  }

  /** True if this button is currently enabled. */
  def enabled = enabledVar

  /**
   * Enables or disables this control.
   */
  def enabled_=(enable: Boolean) {
    enabledVar = enable
    if(rendered) {
      if(enable)
        partialUpdate(JsRaw("$('#" + id + "').removeAttr('disabled')").cmd)
      else
        partialUpdate(JsRaw("$('#" + id + "').attr('disabled', 'disabled')").cmd)
    }
  }

  /** The current label of this button. */
  def label = labelVar

  /**
   * Updates the label of this button.
   */
  def label_=(newLabel: String) {
    labelVar = newLabel
    if(rendered)
      partialUpdate(JsRaw("$('#" + id + " span').html('" + label + "')").cmd)
  }
}