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
    if(rendered) {
      if(!enabled && enable)
        partialUpdate(JsRaw("$('#" + id + "').removeAttr('disabled')").cmd)
      else if(enabled && !enable)
        partialUpdate(JsRaw("$('#" + id + "').attr('disabled', 'disabled')").cmd)
    }

    enabledVar = enable
  }

  /** The current label of this button. */
  def label = labelVar

  /**
   * Updates the label of this button.
   */
  def label_=(newLabel: String) {
    if(rendered && label != newLabel)
      partialUpdate(JsRaw("$('#" + id + " span').html('" + label + "')").cmd)

    labelVar = newLabel
  }
}