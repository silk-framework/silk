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

package de.fuberlin.wiwiss.silk.workbench.lift.util

import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.{Script, OnLoad}
import net.liftweb.http.SHtml
import java.util.UUID
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.common.{Full, Empty}
import xml.{Attribute, Text, Null, NodeSeq}

/**
 * An input field.
 */
sealed trait Field[+T] {
  val label: String

  val description: String

  def value: T

  /**The id of this field */
  protected lazy val id: String = UUID.randomUUID.toString

  def render: NodeSeq

  def updateValueCmd: JsCmd
}

/**
 * A field which holds a string value
 */
case class StringField(label: String, description: String, initialValue: () => String = (() => "")) extends Field[String] {
  private var v = initialValue()

  override def value = v

  override def render = SHtml.text(v, v = _, "id" -> id, "size" -> "60", "title" -> description)

  override def updateValueCmd = {
    v = initialValue()
    JsRaw("$('#" + id + "').val('" + value + "');").cmd
  }
}

/**
 * A field which holds an integer value
 */
case class IntField(label: String, description: String, min: Int, max: Int, initialValue: () => Int = (() => 0)) extends Field[Int] {
  private var v = initialValue()

  override def value = v

  override def render = SHtml.number(v, (num: Int) => { v = num }, min, max, "id" -> id, "title" -> description, "style" -> "margin-left:0")

  override def updateValueCmd = {
    v = initialValue()
    JsRaw("$('#" + id + "').val('" + value + "');").cmd
  }
}

/**
 * A field which holds a boolean value
 */
case class BooleanField(label: String,
                        description: String,
                        initialValue: () => Boolean = (() => false),
                        onUpdate: Boolean => JsCmd = (_ => JS.Empty)) extends Field[Boolean] {
  private var v = initialValue()

  override def value = v

  override def render = {
    SHtml.ajaxCheckbox(v, updateValue _, "id" -> id, "size" -> "60", "title" -> description, "id" -> id) ++
    <label for={id}>Enable</label>
  }

  override def updateValueCmd = {
    v = initialValue()
    JsRaw("$('#" + id + "').val('" + value + "');").cmd
  }

  def enableCmd(enable: Boolean) = {
    if(enable)
      JsRaw("$('#" + id + "').removeAttr('disabled')").cmd
    else
      JsRaw("$('#" + id + "').attr('disabled', 'disabled')").cmd
  }

  private def updateValue(newValue: Boolean) = {
    v = newValue
    onUpdate(newValue)
  }
}

/**
 * Multiple checkboxes
 */
case class CheckboxesField(label: String,
                           description: String,
                           allowedValues: Seq[String],
                           initialValue: () => Set[String]) extends Field[Set[String]] {
  private var v = initialValue()

  override def value = v

  override def render = {
    val boxes = for((text, index) <- allowedValues.zipWithIndex) yield renderBox(text, index)
    boxes.reduce(_ ++ _)
  }

  private def renderBox(text: String, index: Int) = {
    def update(b: String) {
      if(b == "on")
        v += text
      else
        v -= text
    }

    val input = <input onchange={SHtml.ajaxCall(JsRaw("this.value"), update)._2.cmd.toJsCmd} id={id + index} type="checkbox" />
    val checkedInput = if(value.contains(text)) input % Attribute("checked", Text("checked"), Null) else input

    checkedInput ++ <label for={id + index}>{text}</label>
  }

  override def updateValueCmd = {
    v = initialValue()
    JsRaw("$('#" + id + "').val('" + value + "');").cmd
  }
}

/**
 * A field which holds an enumeration of values which can be selected using buttons.
 */
case class RadioField(label: String, description: String, allowedValues: Seq[String], initialValue: () => String) extends Field[String] {
  private var v = initialValue()

  override def value = v

  override def render = {
    val buttons = for((text, index) <- allowedValues.zipWithIndex) yield renderButton(text, index)

    <div id={id}>{
      buttons.reduce(_ ++ _)
    }</div> ++
    Script(OnLoad(JsRaw("$(\"#" + id + "\").buttonset();").cmd))
  }

  private def renderButton(text: String, index: Int) = {
    def update() = {
      v = text
      JS.Empty
    }

    val input = <input onchange={SHtml.ajaxInvoke(update)._2.cmd.toJsCmd} id={id + index} type="radio" name="selectLinks" />

    val selectedInput = if(text == value) input % Attribute("checked", Text("checked"), Null) else input

    selectedInput ++ <label for={id + index}>{text}</label>
  }

  override def updateValueCmd = {
    JS.Empty
  }
}

/**
 * A field which holds an enumeration of values which can be selected using a drop-down menu.
 */
case class SelectField(label: String, description: String, allowedValues: () => Seq[String], initialValue: () => String) extends Field[String] {
  private var v = initialValue()

  override def value = v

  override def render = SHtml.untrustedSelect(Nil, Empty, v = _, "id" -> id, "title" -> description)

  override def updateValueCmd = {
    v = initialValue()

    //Generate the options of the select box
    val options =
      for(allowedValue <- allowedValues()) yield {
        if(allowedValue == value)
          <option value={allowedValue} selected="true">{allowedValue}</option>
        else
          <option value={allowedValue}>{allowedValue}</option>
      }

    //Update select box
    JsRaw("$('#" + id + "').children().remove();").cmd &
    JsRaw("$('#" + id + "').append('" + options.mkString + "');").cmd
  }
}

