package de.fuberlin.wiwiss.silk.workbench.lift.util

import net.liftweb.http.{SHtml, CometActor}
import xml.Text
import java.util.UUID
import net.liftweb.http.js.JE.JsRaw

/**
 * A button whose properties can be changed at runtime.
 */
trait DynamicButton extends CometActor  {

  /** The id of the HTML button. */
  private val id = UUID.randomUUID.toString

  /** The current label of this button. */
  private var labelVar = "Button"

  /** True if this button is currently enabled. */
  private var enabledVar = true

  /**
   * Called if the button has been pressed.
   */
  protected def onPressed()

  /**
   * Renders this button.
   */
  override def render = {
    def buttonPressed() = {
      onPressed()
      JS.Empty
    }

    if(enabled)
      SHtml.ajaxButton(Text(label), buttonPressed _, ("id" -> id))
    else
      SHtml.ajaxButton(Text(label), buttonPressed _, ("id" -> id), ("disabled" -> "disabled"))
  }

  /** True if this button is currently enabled. */
  def enabled = enabledVar

  /**
   * Enables or disables this control.
   */
  def enabled_=(enable: Boolean) {
    enabledVar = enable
    if(enable)
      partialUpdate(JsRaw("$('#" + id + "').removeAttr('disabled')").cmd)
    else
      partialUpdate(JsRaw("$('#" + id + "').attr('disabled', 'disabled')").cmd)
  }

  /** The current label of this button. */
  def label = labelVar

  /**
   * Updates the label of this button.
   */
  def label_=(newLabel: String) {
    labelVar = newLabel
    partialUpdate(JsRaw("$('#" + id + " span').html('" + label + "')").cmd)
  }
}